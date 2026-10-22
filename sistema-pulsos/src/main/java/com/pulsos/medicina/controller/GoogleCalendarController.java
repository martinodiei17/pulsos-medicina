package com.pulsos.medicina.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pulsos.medicina.model.Rol;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.GoogleCalendarCredentialRepository;
import com.pulsos.medicina.repository.GoogleCalendarSyncJobRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;
import com.pulsos.medicina.service.GoogleCalendarOAuthService;
import com.pulsos.medicina.service.GoogleCalendarSyncJobService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/google/calendar")
public class GoogleCalendarController {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarController.class);
    private static final String SESSION_STATE = "GOOGLE_CALENDAR_OAUTH_STATE";
    private static final String SESSION_USER_ID = "GOOGLE_CALENDAR_OAUTH_USER_ID";
    private static final List<String> ESTADOS_NO_TERMINADOS =
            List.of("PENDIENTE", "ERROR", "PROCESANDO");

    private final GoogleCalendarOAuthService oauthService;
    private final GoogleCalendarSyncJobService syncJobService;
    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;
    private final GoogleCalendarCredentialRepository credentialRepository;
    private final GoogleCalendarSyncJobRepository syncJobRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public GoogleCalendarController(
            GoogleCalendarOAuthService oauthService,
            GoogleCalendarSyncJobService syncJobService,
            UsuarioRepository usuarioRepository,
            TurnoRepository turnoRepository,
            GoogleCalendarCredentialRepository credentialRepository,
            GoogleCalendarSyncJobRepository syncJobRepository) {
        this.oauthService = oauthService;
        this.syncJobService = syncJobService;
        this.usuarioRepository = usuarioRepository;
        this.turnoRepository = turnoRepository;
        this.credentialRepository = credentialRepository;
        this.syncJobRepository = syncJobRepository;
    }

    @GetMapping("/conectar")
    public String conectar(Authentication authentication,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerMedicoAutenticado(authentication);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "Solo un profesional médico activo puede conectar Google Calendar.");
            return "redirect:/turnos";
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_USER_ID, usuario.getId());

        try {
            return "redirect:" + oauthService.generarUrlAutorizacion(state);
        } catch (Exception e) {
            session.removeAttribute(SESSION_STATE);
            session.removeAttribute(SESSION_USER_ID);
            log.error("No se pudo iniciar OAuth de Google Calendar para usuario {}",
                    usuario.getId(), e);
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "No se pudo iniciar la conexión con Google Calendar. Revisá la configuración.");
            return "redirect:/turnos";
        }
    }

    @GetMapping("/callback")
    public String callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Object expectedState = session.getAttribute(SESSION_STATE);
        Object expectedUserId = session.getAttribute(SESSION_USER_ID);
        session.removeAttribute(SESSION_STATE);
        session.removeAttribute(SESSION_USER_ID);

        if (!(expectedState instanceof String)
                || !(expectedUserId instanceof Long)
                || state == null
                || !MessageDigest.isEqual(
                        ((String) expectedState).getBytes(StandardCharsets.UTF_8),
                        state.getBytes(StandardCharsets.UTF_8))) {
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "La autorización de Google no es válida o expiró. Intentá nuevamente.");
            return "redirect:/turnos";
        }

        Usuario usuario = obtenerMedicoAutenticado(authentication);
        if (usuario == null || !usuario.getId().equals(expectedUserId)) {
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "La sesión del profesional cambió. Volvé a conectar Google Calendar.");
            return "redirect:/turnos";
        }
        if (error != null && !error.isBlank()) {
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "La autorización de Google Calendar fue cancelada o rechazada.");
            return "redirect:/turnos";
        }
        if (code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "Google no devolvió el código de autorización.");
            return "redirect:/turnos";
        }

        try {
            JsonNode tokens = oauthService.intercambiarCodigo(code);
            oauthService.guardarConexion(usuario.getId(), tokens);
        } catch (Exception e) {
            log.error("Falló la conexión Google Calendar para usuario {}", usuario.getId(), e);
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "No se pudo conectar Google Calendar. Verificá que el correo de Google "
                            + "coincida con el registrado para el profesional e intentá nuevamente.");
            return "redirect:/turnos";
        }

        try {
            int cantidad = registrarTurnosExistentes(usuario.getId());
            redirectAttributes.addFlashAttribute("googleCalendarExito",
                    "¡Google Calendar conectado correctamente! " + cantidad
                            + " turnos nuevos incorporados a la cola de sincronización.");
        } catch (Exception e) {
            log.error("Error al preparar turnos existentes para usuario {}", usuario.getId(), e);
            redirectAttributes.addFlashAttribute("googleCalendarExito",
                    "Google Calendar quedó conectado correctamente.");
            redirectAttributes.addFlashAttribute("googleCalendarError",
                    "No se pudieron preparar todos los turnos existentes. "
                            + "Consultá los registros del servidor antes de volver a intentar.");
        }
        return "redirect:/turnos";
    }

    /**
     * Agrega únicamente turnos que aún no tienen evento remoto ni
     * operaciones en curso. No vuelve a crear eventos al reconectar.
     */
    private int registrarTurnosExistentes(Long usuarioId) {
        int registrados = 0;
        for (Turno turno : turnoRepository.findAll()) {
            Usuario medico = resolverMedico(turno);
            if (medico == null || !usuarioId.equals(medico.getId())
                    || esCancelado(turno) || turno.getFechaHora() == null) {
                continue;
            }
            if (turno.getGoogleCalendarEventId() != null
                    && !turno.getGoogleCalendarEventId().isBlank()) {
                continue;
            }
            // Evita agregar un segundo trabajo si ya hay uno pendiente,
            // fallido (en reintento) o en procesamiento para este turno.
            // Esta comprobación se realiza para la única instancia actual.
            if (hayTrabajoNoTerminado(turno.getId())) {
                continue;
            }
            syncJobService.registrarTrabajo(turno.getId(), usuarioId,
                    "CREAR", null, null);
            registrados++;
        }
        return registrados;
    }

    private boolean hayTrabajoNoTerminado(Long turnoId) {
        return syncJobRepository.existsByTurnoIdAndEstadoIn(turnoId, ESTADOS_NO_TERMINADOS);
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
        return estado != null && ("CANCELADO".equalsIgnoreCase(estado)
                || "CANCELADA".equalsIgnoreCase(estado));
    }

    @GetMapping("/estado")
    @ResponseBody
    public String estado(Authentication authentication) {
        Usuario usuario = obtenerMedicoAutenticado(authentication);
        if (usuario == null) {
            return "NO_AUTORIZADO";
        }
        boolean credencialExiste = credentialRepository.findByUsuarioId(usuario.getId())
                .map(c -> c.getRefreshTokenCifrado() != null
                        && !c.getRefreshTokenCifrado().isBlank())
                .orElse(false);
        return usuario.isGoogleCalendarConectado() && credencialExiste
                ? "CONECTADO" : "DESCONECTADO";
    }

    private Usuario obtenerMedicoAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        Optional<Usuario> usuarioOptional =
                usuarioRepository.findByUsername(authentication.getName());
        if (usuarioOptional.isEmpty()) {
            return null;
        }
        Usuario usuario = usuarioOptional.get();
        if (!usuario.isActivo() || usuario.getRoles() == null
                || !usuario.getRoles().contains(Rol.ROLE_MEDICO)) {
            return null;
        }
        return usuario;
    }
}
