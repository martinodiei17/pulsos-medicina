package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.service.TurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/turnos")
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarTurno(@ModelAttribute Turno turno, Authentication authentication) {
        try {
            Turno nuevoTurno = turnoService.guardarTurno(turno);
            
            // Identificador o email del usuario logueado actual para disparar el calendario
            String identificadorUsuario = (authentication != null) ? authentication.getName() : "default";

            return ResponseEntity.ok().body(new TurnoRespuestaDto(true, nuevoTurno.getId(), identificadorUsuario, "Turno guardado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new TurnoRespuestaDto(false, null, null, e.getMessage()));
        }
    }

    public static class TurnoRespuestaDto {
        private boolean success;
        private Long idTurno;
        private String identificador;
        private String mensaje;

        public TurnoRespuestaDto(boolean success, Long idTurno, String identificador, String mensaje) {
            this.success = success;
            this.idTurno = idTurno;
            this.identificador = identificador;
            this.mensaje = mensaje;
        }

        public boolean isSuccess() { return success; }
        public Long getIdTurno() { return idTurno; }
        public String getIdentificador() { return identificador; }
        public String getMensaje() { return mensaje; }
    }
}
