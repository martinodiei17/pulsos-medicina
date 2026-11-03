package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.service.PacienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class BusquedaPacienteController {

    private final PacienteService pacienteService;

    public BusquedaPacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping("/api/pacientes/buscar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MEDICO')")
    public ResponseEntity<List<Map<String, Object>>> buscar(
            @RequestParam(value = "q", required = false) String q) {

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        String busqueda = q.trim();

        if (busqueda.length() > 120) {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, Object>> resultados = pacienteService
                .listarTodos(busqueda)
                .stream()
                .sorted(
                    Comparator.comparing(
                        Paciente::getNombreCompleto,
                        Comparator.nullsLast(
                            String.CASE_INSENSITIVE_ORDER
                        )
                    )
                )
                .map(p -> Map.<String, Object>of(
                    "id", p.getId(),
                    "nombre", p.getNombreCompleto(),
                    "dni", p.getDni()
                ))
                .toList();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(resultados);
    }
}
