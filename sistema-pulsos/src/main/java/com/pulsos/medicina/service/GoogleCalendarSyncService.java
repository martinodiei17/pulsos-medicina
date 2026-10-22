package com.pulsos.medicina.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pulsos.medicina.model.GoogleCalendarCredential;
import com.pulsos.medicina.model.GoogleCalendarSyncJob;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.GoogleCalendarCredentialRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class GoogleCalendarSyncService {

    private static final String API_URL =
            "https://www.googleapis.com/calendar/v3/calendars/";
    private static final String EVENT_TITLE = "Turno médico - Pulsos Medicina";
    private static final String EVENT_DESCRIPTION =
            "Turno programado desde Pulsos Medicina.";
    private static final int DURATION_MINUTES = 45;

    private final GoogleCalendarOAuthService oauthService;
    private final GoogleCalendarCredentialRepository credentialRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ZoneId zoneId;

    public GoogleCalendarSyncService(
            GoogleCalendarOAuthService oauthService,
            GoogleCalendarCredentialRepository credentialRepository,
            TurnoRepository turnoRepository,
            UsuarioRepository usuarioRepository,
            ObjectMapper objectMapper,
            @Value("${pulsos.calendar.time-zone:America/Argentina/Buenos_Aires}")
            String timeZone) {
        this.oauthService = oauthService;
        this.credentialRepository = credentialRepository;
        this.turnoRepository = turnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
        this.zoneId = ZoneId.of(timeZone);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Compatibilidad con llamadas existentes. */
    public void sincronizarTurno(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Turno no encontrado: " + turnoId));
        Usuario medico = resolverMedico(turno);
        if (medico == null) {
            return;
        }
        if (esCancelado(turno)) {
            eliminarEvento(turnoId);
            return;
        }
        sincronizarTurnoParaMedico(turno, medico.getId(), null);
    }

    /**
     * Respeta el médico original registrado en el trabajo.
     * Un trabajo antiguo no debe crear eventos en el calendario del médico nuevo.
     */
    public void sincronizarTrabajo(GoogleCalendarSyncJob job) {
        if (job == null || job.getUsuarioId() == null
                || job.getTurnoId() == null) {
            throw new IllegalArgumentException("Trabajo de sincronización inválido");
        }

        String operacion = job.getOperacion();
        if ("ELIMINAR".equals(operacion)) {
            String eventId = valorOIdDeterministico(
                    job.getGoogleEventId(), job.getUsuarioId(), job.getTurnoId());
            eliminarEventoPorDatos(job.getUsuarioId(),
                    job.getGoogleCalendarId(), eventId);
            return;
        }
        if (!"CREAR".equals(operacion) && !"ACTUALIZAR".equals(operacion)) {
            throw new IllegalArgumentException("Operación no reconocida: " + operacion);
        }

        Turno turno = turnoRepository.findById(job.getTurnoId()).orElse(null);
        if (turno == null || esCancelado(turno)) {
            // La operación quedó obsoleta: no recrear un turno eliminado/cancelado.
            return;
        }

        Usuario medicoActual = resolverMedico(turno);
        if (medicoActual == null || !job.getUsuarioId().equals(medicoActual.getId())) {
            // El turno fue reasignado: este trabajo pertenece al médico anterior.
            return;
        }
        sincronizarTurnoParaMedico(turno, job.getUsuarioId(), job);
    }

    private void sincronizarTurnoParaMedico(
            Turno turno, Long usuarioId, GoogleCalendarSyncJob job) {
        if (turno.getFechaHora() == null) {
            throw new IllegalStateException("El turno no tiene fecha y hora");
        }
        GoogleCalendarCredential credential = obtenerCredencial(usuarioId);
        String calendarId = normalizarCalendarId(credential.getCalendarId());
        String eventId = generarEventId(usuarioId, turno.getId());

        // Los IDs anteriores solo se reutilizan si pertenecen al mismo calendario.
        if (turno.getGoogleCalendarEventId() != null
                && !turno.getGoogleCalendarEventId().isBlank()
                && calendarId.equals(normalizarCalendarId(turno.getGoogleCalendarId()))) {
            eventId = turno.getGoogleCalendarEventId();
        }

        String accessToken = oauthService.obtenerAccessToken(usuarioId);
        String body;
        try {
            ObjectNode evento = construirEvento(turno);
            evento.put("id", eventId);
            body = objectMapper.writeValueAsString(evento);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo preparar el evento", e);
        }

        // POST con ID estable: si ya existe, PUT. No hay recursión entre ambos.
        HttpResponse<String> response = enviar("POST",
                urlEventos(calendarId), accessToken, body);
        if (response.statusCode() == 409) {
            response = enviar("PUT", urlEvento(calendarId, eventId),
                    accessToken, body);
        }
        if (!esExito(response.statusCode())) {
            throw new IllegalStateException(
                    "Google Calendar rechazó el guardado. HTTP "
                            + response.statusCode());
        }

        // No volver a asignar IDs de un médico anterior si el turno cambió
        // mientras Google procesaba la petición.
        Turno actual = turnoRepository.findById(turno.getId()).orElse(null);
        if (actual != null && !esCancelado(actual)) {
            Usuario medicoActual = resolverMedico(actual);
            if (medicoActual != null && usuarioId.equals(medicoActual.getId())) {
                actual.setGoogleCalendarEventId(eventId);
                actual.setGoogleCalendarId(calendarId);
                turnoRepository.save(actual);
            }
        }
    }

    /** Elimina el evento de un turno que todavía existe localmente. */
    public void eliminarEvento(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId).orElse(null);
        if (turno == null) {
            return;
        }
        Usuario medico = resolverMedico(turno);
        if (medico == null) {
            return;
        }
        String eventId = valorOIdDeterministico(
                turno.getGoogleCalendarEventId(), medico.getId(), turnoId);
        eliminarEventoPorDatos(medico.getId(), turno.getGoogleCalendarId(), eventId);
        turno.setGoogleCalendarEventId(null);
        turno.setGoogleCalendarId(null);
        turnoRepository.save(turno);
    }

    /**
     * Permite borrar el evento aunque el turno ya haya sido eliminado de PostgreSQL.
     * HTTP 404/410 significa que el evento ya no existe.
     */
    public void eliminarEventoPorDatos(
            Long usuarioId, String calendarId, String eventId) {
        if (usuarioId == null || eventId == null || eventId.isBlank()) {
            return;
        }
        GoogleCalendarCredential credential = obtenerCredencial(usuarioId);
        String destino = (calendarId == null || calendarId.isBlank())
                ? normalizarCalendarId(credential.getCalendarId())
                : calendarId;
        String accessToken = oauthService.obtenerAccessToken(usuarioId);
        HttpResponse<String> response = enviar("DELETE",
                urlEvento(destino, eventId), accessToken, null);
        int status = response.statusCode();
        if (status != 200 && status != 204 && status != 404 && status != 410) {
            throw new IllegalStateException(
                    "Google Calendar rechazó la eliminación. HTTP " + status);
        }
    }

    /** Compatibilidad con la sincronización manual de todos los turnos. */
    public void sincronizarTurnosDelMedico(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El profesional es obligatorio");
        }
        List<Turno> turnos = turnoRepository.findAll();
        for (Turno turno : turnos) {
            Usuario medico = resolverMedico(turno);
            if (medico != null && usuarioId.equals(medico.getId())) {
                sincronizarTurno(turno.getId());
            }
        }
    }

    private ObjectNode construirEvento(Turno turno) {
        ZonedDateTime inicio = turno.getFechaHora().atZone(zoneId);
        ZonedDateTime fin = inicio.plusMinutes(DURATION_MINUTES);
        ObjectNode evento = objectMapper.createObjectNode();
        evento.put("summary", EVENT_TITLE);
        evento.put("description", EVENT_DESCRIPTION);
        ObjectNode start = evento.putObject("start");
        start.put("dateTime", inicio.toOffsetDateTime().toString());
        start.put("timeZone", zoneId.getId());
        ObjectNode end = evento.putObject("end");
        end.put("dateTime", fin.toOffsetDateTime().toString());
        end.put("timeZone", zoneId.getId());
        return evento;
    }

    private GoogleCalendarCredential obtenerCredencial(Long usuarioId) {
        return credentialRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "El profesional no tiene Google Calendar conectado"));
    }

    private Usuario resolverMedico(Turno turno) {
        if (turno.getMedico() != null) {
            return turno.getMedico();
        }
        String nombre = turno.getMedicoAsignado();
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return usuarioRepository.findByNombreCompleto(nombre).orElse(null);
    }

    private boolean esCancelado(Turno turno) {
        String estado = turno.getEstado();
        return estado != null && ("CANCELADO".equalsIgnoreCase(estado.trim())
                || "CANCELADA".equalsIgnoreCase(estado.trim()));
    }

    private String normalizarCalendarId(String calendarId) {
        return calendarId == null || calendarId.isBlank() ? "primary" : calendarId;
    }

    private String valorOIdDeterministico(
            String eventId, Long usuarioId, Long turnoId) {
        return eventId == null || eventId.isBlank()
                ? generarEventId(usuarioId, turnoId) : eventId;
    }

    private String generarEventId(Long usuarioId, Long turnoId) {
        // Google admite caracteres base32hex: 0-9 y a-v.
        return "pulsos" + Long.toHexString(usuarioId)
                + "a" + Long.toHexString(turnoId) + "b2026";
    }

    private String urlEventos(String calendarId) {
        return API_URL + encode(calendarId) + "/events";
    }

    private String urlEvento(String calendarId, String eventId) {
        return urlEventos(calendarId) + "/" + encode(eventId);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean esExito(int status) {
        return status >= 200 && status < 300;
    }

    private HttpResponse<String> enviar(
            String metodo, String url, String accessToken, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken);
            if (body != null) {
                builder.header("Content-Type", "application/json; charset=UTF-8");
            }
            if ("POST".equals(metodo)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(body));
            } else if ("PUT".equals(metodo)) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(body));
            } else if ("DELETE".equals(metodo)) {
                builder.DELETE();
            } else {
                throw new IllegalArgumentException("Método HTTP no admitido");
            }
            return httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Error de comunicación con Google Calendar", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La solicitud a Google fue interrumpida", e);
        }
    }
}
