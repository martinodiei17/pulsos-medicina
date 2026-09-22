package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.repository.PacienteRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepo;
    private final PacienteRepository pacienteRepo;

    public TurnoService(TurnoRepository turnoRepo, PacienteRepository pacienteRepo) {
        this.turnoRepo = turnoRepo;
        this.pacienteRepo = pacienteRepo;
    }

    public List<Turno> listarTodos() {
        return turnoRepo.findAllByOrderByFechaHoraDesc();
    }

    public List<Turno> listarTurnosHoy() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return turnoRepo.findByFechaHoraBetweenOrderByFechaHoraAsc(start, end);
    }

    public Turno agendarTurno(Long pacienteId, Turno turno) {
        Paciente paciente = pacienteRepo.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        turno.setPaciente(paciente);
        return turnoRepo.save(turno);
    }

    public void actualizarEstado(Long turnoId, String nuevoEstado) {
        Turno turno = turnoRepo.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));
        turno.setEstado(nuevoEstado);
        turnoRepo.save(turno);
    }
}
