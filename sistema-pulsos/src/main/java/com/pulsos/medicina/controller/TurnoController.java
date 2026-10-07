package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.service.PacienteService;
import com.pulsos.medicina.service.TurnoService;
import com.pulsos.medicina.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/turnos")
public class TurnoController {

    private final TurnoService turnoService;
    private final PacienteService pacienteService;
    private final UsuarioService usuarioService;

    public TurnoController(
            TurnoService turnoService,
            PacienteService pacienteService,
            UsuarioService usuarioService) {

        this.turnoService = turnoService;
        this.pacienteService = pacienteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String agenda(Model model, Authentication authentication) {

        model.addAttribute("nuevoTurno", new Turno());
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("medicos", usuarioService.listarMedicos());
        model.addAttribute("turnos", turnoService.listarTodos());

        Usuario usuarioActual = null;

        if (authentication != null && authentication.isAuthenticated()) {
            usuarioActual = usuarioService
                    .buscarPorUsername(authentication.getName())
                    .orElse(null);
        }

        model.addAttribute("usuarioActual", usuarioActual);

        return "turnos/agenda";
    }

    @PostMapping("/agendar")
    public String agendarTurno(
            @RequestParam("pacienteId") Long pacienteId,
            @ModelAttribute("nuevoTurno") Turno turno) {

        turnoService.agendarTurno(pacienteId, turno);

        return "redirect:/turnos";
    }

    @PostMapping("/{id}/estado")
    public String actualizarEstado(
            @PathVariable("id") Long id,
            @RequestParam("estado") String estado) {

        turnoService.actualizarEstado(id, estado);

        return "redirect:/turnos";
    }
}
