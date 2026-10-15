
package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.repository.PacienteRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepo;
    private final PacienteRepository pacienteRepo;
    private final UsuarioRepository usuarioRepo;

    public TurnoService(
            TurnoRepository turnoRepo,
            PacienteRepository pacienteRepo,
            UsuarioRepository usuarioRepo) {

        this.turnoRepo = turnoRepo;
        this.pacienteRepo = pacienteRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<Turno> listarTodos() {
        return turnoRepo.findAllByOrderByFechaHoraDesc();
    }

    public List<Turno> listarTurnosHoy() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        return turnoRepo.findByFechaHoraBetweenOrderByFechaHoraAsc(
                start, end);
    }

    public Turno buscarPorId(Long id) {
        return turnoRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Turno no encontrado: " + id));
    }

    public Turno agendarTurno(Long pacienteId, Turno turno) {

        if (turno == null) {
            throw new IllegalArgumentException(
                    "El turno no puede ser nulo");
        }

        Paciente paciente = pacienteRepo.findById(pacienteId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Paciente no encontrado"));

        turno.setId(null);
        turno.setPaciente(paciente);

        return turnoRepo.save(turno);
    }

    public Turno guardarTurno(Turno turno) {

        if (turno == null) {
            throw new IllegalArgumentException(
                    "El turno no puede ser nulo");
        }

        return turnoRepo.save(turno);
    }

    @Transactional
    public Turno editarTurno(
            Long turnoId,
            Long pacienteId,
            Turno datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del turno no pueden ser nulos");
        }

        Turno existente = buscarPorId(turnoId);

        Paciente paciente = pacienteRepo.findById(pacienteId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Paciente no encontrado"));

        if (datos.getFechaHora() == null) {
            throw new IllegalArgumentException(
                    "La fecha y hora son obligatorias");
        }

        existente.setPaciente(paciente);
        existente.setFechaHora(datos.getFechaHora());
        existente.setMedicoAsignado(
                datos.getMedicoAsignado());
        existente.setEspecialidad(
                datos.getEspecialidad());
        existente.setMotivo(datos.getMotivo());

        // Conservamos el estado actual del turno.
        // Se modifica por la ruta /turnos/{id}/estado.

        return turnoRepo.save(existente);
    }

    @Transactional
    public void eliminarTurno(Long turnoId) {

        Turno turno = buscarPorId(turnoId);

        turnoRepo.delete(turno);
    }

    @Transactional
    public void actualizarEstado(
            Long turnoId,
            String nuevoEstado) {

        Turno turno = buscarPorId(turnoId);

        turno.setEstado(nuevoEstado);

        turnoRepo.save(turno);
    }
}
