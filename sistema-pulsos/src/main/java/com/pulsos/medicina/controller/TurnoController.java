package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.service.TurnoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/turnos")
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @GetMapping
    public String listarTurnos(Model model) {
        model.addAttribute("turnos", turnoService.listarTodos());
        return "turnos/agenda";
    }

    @PostMapping("/guardar")
    public String guardarTurno(@ModelAttribute Turno turno, @RequestParam(value = "pacienteId", required = false) Long pacienteId) {
        // Obtenemos el ID del paciente (ya sea enviado por parámetro o dentro del objeto Turno)
        Long idPaciente = (pacienteId != null) ? pacienteId : 
                          (turno.getPaciente() != null ? turno.getPaciente().getId() : null);

        if (idPaciente != null) {
            // Llamamos a agendarTurno que es el método existente en tu TurnoService
            turnoService.agendarTurno(idPaciente, turno);
        }

        return "redirect:/turnos";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        turnoService.actualizarEstado(id, estado);
        return "redirect:/turnos";
    }
}
