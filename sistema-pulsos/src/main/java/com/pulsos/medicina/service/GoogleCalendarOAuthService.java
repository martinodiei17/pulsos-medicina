package com.pulsos.medicina.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsos.medicina.model.GoogleCalendarCredential;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.GoogleCalendarCredentialRepository;
import com.pulsos.medicina.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleCalendarOAuthService {

    private static final String AUTH_URL =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String TOKEN_URL =
            "https://oauth2.googleapis.com/token";

    private static final String USERINFO_URL =
            "https://openidconnect.googleapis.com/v1/userinfo";

    private static final String SCOPE =
            "openid email https://www.googleapis.com/auth/calendar.events";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private final GoogleCalendarCredentialRepository credentialRepository;
    private final UsuarioRepository usuarioRepository;
    private final GoogleTokenEncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GoogleCalendarOAuthService(
            @Value("${GOOGLE_CLIENT_ID}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET}") String clientSecret,
            @Value("${GOOGLE_REDIRECT_URI}") String redirectUri,
            GoogleCalendarCredentialRepository credentialRepository,
            UsuarioRepository usuarioRepository,
            GoogleTokenEncryptionService encryptionService,
            ObjectMapper objectMapper) {

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.credentialRepository = credentialRepository;
        this.usuarioRepository = usuarioRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Construye la URL para que el profesional autorice
     * el acceso a su Google Calendar.
     *
     * El parámetro state debe ser aleatorio, estar asociado
     * a la sesión del usuario y verificarse en el callback.
     */
    public String generarUrlAutorizacion(String state) {

        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado OAuth es obligatorio"
            );
        }

        return AUTH_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);
    }

    /**
     * Intercambia el código de autorización por tokens.
     */
    public JsonNode intercambiarCodigo(String code) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Google no devolvió un código de autorización"
            );
        }

        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");

        return solicitarToken(params);
    }

    /**
     * Obtiene un nuevo access token mediante el refresh token
     * almacenado de forma cifrada.
     */
    public String obtenerAccessToken(Long usuarioId) {

        GoogleCalendarCredential credential =
                credentialRepository.findByUsuarioId(usuarioId)
                        .orElseThrow(() -> new IllegalStateException(
                                "El profesional no tiene Google Calendar vinculado"
                        ));

        String refreshToken = encryptionService.decrypt(
                credential.getRefreshTokenCifrado()
        );

        Map<String, String> params = new HashMap<>();
        params.put("refresh_token", refreshToken);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "refresh_token");

        JsonNode response = solicitarToken(params);

        String accessToken = response.path("access_token").asText("");

        if (accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Google no devolvió un access token válido"
            );
        }

        return accessToken;
    }

    /**
     * Consulta a Google la identidad de la cuenta autorizada.
     * No se confía en un correo enviado desde el navegador.
     */
    public String obtenerEmailGoogle(String accessToken) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "No se pudo verificar la identidad de Google"
                );
            }

            JsonNode userInfo = objectMapper.readTree(response.body());

            if (!userInfo.path("email_verified").asBoolean(false)) {
                throw new IllegalStateException(
                        "La cuenta Google no tiene el correo verificado"
                );
            }

            String email = userInfo.path("email").asText("");

            if (email.isBlank()) {
                throw new IllegalStateException(
                        "Google no devolvió el correo de la cuenta"
                );
            }

            return email;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error de comunicación al consultar la cuenta Google",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "La consulta a Google fue interrumpida",
                    e
            );
        }
    }

    /**
     * Guarda o actualiza la conexión OAuth del profesional.
     *
     * Nunca almacena el refresh token sin cifrar.
     */
    @Transactional
    public void guardarConexion(
            Long usuarioId,
            JsonNode tokenResponse) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profesional no encontrado"
                ));

        if (!usuario.isActivo()) {
            throw new IllegalStateException(
                    "El usuario está inactivo"
            );
        }

        String accessToken = tokenResponse
                .path("access_token").asText("");

        String refreshToken = tokenResponse
                .path("refresh_token").asText("");

        if (accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Google no devolvió un access token"
            );
        }

        String googleEmail = obtenerEmailGoogle(accessToken);

        if (usuario.getEmail() == null
                || usuario.getEmail().isBlank()
                || !usuario.getEmail().trim()
                        .equalsIgnoreCase(googleEmail.trim())) {

            throw new IllegalStateException(
                    "El correo de Google debe coincidir con " +
                    "el correo registrado del profesional"
            );
        }

        GoogleCalendarCredential credential =
                credentialRepository.findByUsuarioId(usuarioId)
                        .orElseGet(GoogleCalendarCredential::new);

        if (refreshToken.isBlank()) {

            if (credential.getRefreshTokenCifrado() == null
                    || credential.getRefreshTokenCifrado().isBlank()) {

                throw new IllegalStateException(
                        "Google no devolvió un refresh token. " +
                        "Volvé a autorizar la cuenta."
                );
            }

            if (credential.getGoogleEmail() != null
                    && !credential.getGoogleEmail()
                            .equalsIgnoreCase(googleEmail)) {

                throw new IllegalStateException(
                        "No se puede reutilizar el token " +
                        "de otra cuenta Google"
                );
            }

        } else {
            credential.setRefreshTokenCifrado(
                    encryptionService.encrypt(refreshToken)
            );
        }

        credential.setUsuario(usuario);
        credential.setGoogleEmail(googleEmail);
        credential.setCalendarId("primary");
        credential.setUltimoError(null);
        credential.setUltimoErrorFecha(null);

        credentialRepository.save(credential);

        usuario.setGoogleCalendarConectado(true);
        usuario.setGoogleCalendarEmail(googleEmail);

        usuarioRepository.save(usuario);
    }

    /**
     * Elimina las credenciales locales de vinculación.
     *
     * No revoca el acceso en Google ni elimina eventos remotos.
     * Esas operaciones deberán gestionarse por separado.
     */
    @Transactional
    public void eliminarConexionLocal(Long usuarioId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profesional no encontrado"
                ));

        credentialRepository.deleteByUsuarioId(usuarioId);

        usuario.setGoogleCalendarConectado(false);
        usuario.setGoogleCalendarEmail(null);
        usuario.setGoogleCalendarUltimaSync(null);

        usuarioRepository.save(usuario);
    }

    /**
     * Envía una solicitud al endpoint OAuth de Google.
     */
    private JsonNode solicitarToken(Map<String, String> params) {

        try {
            StringBuilder body = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {

                if (body.length() > 0) {
                    body.append("&");
                }

                body.append(encode(entry.getKey()))
                        .append("=")
                        .append(encode(entry.getValue()));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    body.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Google rechazó la solicitud OAuth. " +
                        "Código HTTP: " + response.statusCode()
                );
            }

            JsonNode json = objectMapper.readTree(response.body());

            if (json.path("access_token").asText("").isBlank()) {
                throw new IllegalStateException(
                        "La respuesta OAuth no contiene access_token"
                );
            }

            return json;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error de comunicación con Google OAuth",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "La solicitud OAuth fue interrumpida",
                    e
            );
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
