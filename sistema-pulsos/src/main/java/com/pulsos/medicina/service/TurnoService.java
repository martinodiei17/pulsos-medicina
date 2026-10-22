package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.PacienteRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepo;
    private final PacienteRepository pacienteRepo;
    private final UsuarioRepository usuarioRepo;
    private final GoogleCalendarSyncJobService syncJobService;

    public TurnoService(TurnoRepository turnoRepo,
                        PacienteRepository pacienteRepo,
                        UsuarioRepository usuarioRepo,
                        GoogleCalendarSyncJobService syncJobService) {
        this.turnoRepo = turnoRepo;
        this.pacienteRepo = pacienteRepo;
        this.usuarioRepo = usuarioRepo;
        this.syncJobService = syncJobService;
    }

    public List<Turno> listarTodos() {
        return turnoRepo.findAllByOrderByFechaHoraDesc();
    }

    public List<Turno> listarTurnosHoy() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return turnoRepo.findByFechaHoraBetweenOrderByFechaHoraAsc(inicio, fin);
    }

    public Turno buscarPorId(Long id) {
        return turnoRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Turno no encontrado: " + id));
    }

    @Transactional
    public Turno agendarTurno(Long pacienteId, Turno turno) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser nulo");
        }
        validarFecha(turno);
        turno.setId(null);
        turno.setPaciente(buscarPaciente(pacienteId));
        turno.setMedico(resolverMedico(turno.getMedicoAsignado()));
        turno.setGoogleCalendarEventId(null);
        turno.setGoogleCalendarId(null);
        if (turno.getEstado() == null || turno.getEstado().isBlank()) {
            turno.setEstado("PENDIENTE");
        }
        Turno guardado = turnoRepo.saveAndFlush(turno);
        registrarCreacionSiCorresponde(guardado);
        return guardado;
    }

    @Transactional
    public Turno guardarTurno(Turno turno) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser nulo");
        }
        if (turno.getPaciente() == null || turno.getPaciente().getId() == null) {
            throw new IllegalArgumentException("El paciente es obligatorio");
        }
        if (turno.getId() == null) {
            return agendarTurno(turno.getPaciente().getId(), turno);
        }
        return editarTurno(turno.getId(), turno.getPaciente().getId(), turno);
    }

    @Transactional
    public Turno editarTurno(Long turnoId, Long pacienteId, Turno datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del turno no pueden ser nulos");
        }
        validarFecha(datos);

        Turno existente = buscarPorId(turnoId);
        Paciente paciente = buscarPaciente(pacienteId);
        Usuario medicoAnterior = resolverMedicoDelTurno(existente);
        Usuario medicoNuevo = resolverMedico(datos.getMedicoAsignado());
        boolean cambioMedico = !Objects.equals(
                idUsuario(medicoAnterior), idUsuario(medicoNuevo));
        boolean estabaCancelado = esCancelado(existente.getEstado());
        boolean quedaraCancelado = esCancelado(datos.getEstado());

        String eventIdAnterior = existente.getGoogleCalendarEventId();
        String calendarIdAnterior = existente.getGoogleCalendarId();

        // Se registra la eliminación ANTES de la nueva creación para
        // respetar el orden de los trabajos de este turno.
        if ((cambioMedico || (!estabaCancelado && quedaraCancelado))
                && debeEliminarEnGoogle(medicoAnterior, eventIdAnterior)) {
            registrarEliminacion(existente.getId(), medicoAnterior,
                    eventIdAnterior, calendarIdAnterior);
        }

        existente.setPaciente(paciente);
        existente.setFechaHora(datos.getFechaHora());
        existente.setMedicoAsignado(datos.getMedicoAsignado());
        existente.setMedico(medicoNuevo);
        existente.setEspecialidad(datos.getEspecialidad());
        existente.setMotivo(datos.getMotivo());
        if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
            existente.setEstado(datos.getEstado().trim().toUpperCase(Locale.ROOT));
        }

        if (cambioMedico || quedaraCancelado) {
            existente.setGoogleCalendarEventId(null);
            existente.setGoogleCalendarId(null);
        }

        Turno guardado = turnoRepo.saveAndFlush(existente);
        if (!esCancelado(guardado.getEstado()) && medicoNuevo != null
                && medicoNuevo.isGoogleCalendarConectado()) {
            String operacion = (cambioMedico || estabaCancelado
                    || sinValor(guardado.getGoogleCalendarEventId()))
                    ? "CREAR" : "ACTUALIZAR";
            syncJobService.registrarTrabajo(guardado.getId(), medicoNuevo.getId(),
                    operacion, guardado.getGoogleCalendarEventId(),
                    guardado.getGoogleCalendarId());
        }
        return guardado;
    }

    @Transactional
    public void eliminarTurno(Long turnoId) {
        Turno existente = buscarPorId(turnoId);
        Usuario medico = resolverMedicoDelTurno(existente);
        if (debeEliminarEnGoogle(medico, existente.getGoogleCalendarEventId())) {
            registrarEliminacion(existente.getId(), medico,
                    existente.getGoogleCalendarEventId(),
                    existente.getGoogleCalendarId());
        }
        turnoRepo.delete(existente);
        turnoRepo.flush();
    }

    @Transactional
    public Turno actualizarEstado(Long turnoId, String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        Turno existente = buscarPorId(turnoId);
        Usuario medico = resolverMedicoDelTurno(existente);
        boolean estabaCancelado = esCancelado(existente.getEstado());
        boolean quedaraCancelado = esCancelado(nuevoEstado);
        String eventIdAnterior = existente.getGoogleCalendarEventId();
        String calendarIdAnterior = existente.getGoogleCalendarId();

        if (!estabaCancelado && quedaraCancelado
                && debeEliminarEnGoogle(medico, eventIdAnterior)) {
            registrarEliminacion(existente.getId(), medico,
                    eventIdAnterior, calendarIdAnterior);
        }

        existente.setEstado(nuevoEstado.trim().toUpperCase(Locale.ROOT));
        if (quedaraCancelado) {
            existente.setGoogleCalendarEventId(null);
            existente.setGoogleCalendarId(null);
        }
        Turno guardado = turnoRepo.saveAndFlush(existente);
        if (!quedaraCancelado && medico != null
                && medico.isGoogleCalendarConectado()) {
            String operacion = (estabaCancelado || sinValor(guardado.getGoogleCalendarEventId()))
                    ? "CREAR" : "ACTUALIZAR";
            syncJobService.registrarTrabajo(guardado.getId(), medico.getId(),
                    operacion, guardado.getGoogleCalendarEventId(),
                    guardado.getGoogleCalendarId());
        }
        return guardado;
    }

    private void registrarCreacionSiCorresponde(Turno turno) {
        Usuario medico = resolverMedicoDelTurno(turno);
        if (medico != null && medico.isGoogleCalendarConectado()
                && !esCancelado(turno.getEstado())) {
            syncJobService.registrarTrabajo(turno.getId(), medico.getId(),
                    "CREAR", null, null);
        }
    }

    private void registrarEliminacion(Long turnoId, Usuario medico,
                                     String eventId, String calendarId) {
        if (medico != null) {
            // GoogleCalendarSyncService puede reconstruir el ID estable
            // incluso cuando el evento todavía no quedó guardado en PostgreSQL.
            syncJobService.registrarTrabajo(turnoId, medico.getId(),
                    "ELIMINAR", eventId, calendarId);
        }
    }

    private boolean debeEliminarEnGoogle(Usuario medico, String eventId) {
        return medico != null
                && (medico.isGoogleCalendarConectado() || !sinValor(eventId));
    }

    private Usuario resolverMedicoDelTurno(Turno turno) {
        if (turno.getMedico() != null) {
            return turno.getMedico();
        }
        return resolverMedico(turno.getMedicoAsignado());
    }

    private Usuario resolverMedico(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return null;
        }
        return usuarioRepo.findByNombreCompleto(nombreCompleto).orElse(null);
    }

    private Paciente buscarPaciente(Long pacienteId) {
        if (pacienteId == null) {
            throw new IllegalArgumentException("El paciente es obligatorio");
        }
        return pacienteRepo.findById(pacienteId).orElseThrow(() ->
                new IllegalArgumentException("Paciente no encontrado: " + pacienteId));
    }

    private Long idUsuario(Usuario usuario) {
        return usuario == null ? null : usuario.getId();
    }

    private boolean sinValor(String valor) {
        return valor == null || valor.isBlank();
    }

    private boolean esCancelado(String estado) {
        return estado != null && ("CANCELADO".equalsIgnoreCase(estado.trim())
                || "CANCELADA".equalsIgnoreCase(estado.trim()));
    }

    private void validarFecha(Turno turno) {
        if (turno.getFechaHora() == null) {
            throw new IllegalArgumentException("La fecha y hora son obligatorias");
        }
    }
}
