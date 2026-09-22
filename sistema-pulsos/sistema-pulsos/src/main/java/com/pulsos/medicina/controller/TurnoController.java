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

    public TurnoController(TurnoService turnoService, PacienteService pacienteService, UsuarioService usuarioService) {
        this.turnoService = turnoService;
        this.pacienteService = pacienteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String agenda(Model model, Authentication auth) {
        model.addAttribute("turnos", turnoService.listarTodos());
        model.addAttribute("turnosHoy", turnoService.listarTurnosHoy());
        model.addAttribute("pacientes", pacienteService.listarTodos(null));
        model.addAttribute("medicos", usuarioService.listarMedicos());
        model.addAttribute("nuevoTurno", new Turno());

        if (auth != null) {
            Usuario usuarioActual = usuarioService.buscarPorUsername(auth.getName()).orElse(null);
            model.addAttribute("usuarioActual", usuarioActual);
        }

        return "turnos/agenda";
    }

    @PostMapping("/agendar")
    public String agendar(@RequestParam("pacienteId") Long pacienteId, @ModelAttribute Turno nuevoTurno) {
        turnoService.agendarTurno(pacienteId, nuevoTurno);
        return "redirect:/turnos";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam("estado") String nuevoEstado) {
        turnoService.actualizarEstado(id, nuevoEstado);
        return "redirect:/turnos";
    }
}
