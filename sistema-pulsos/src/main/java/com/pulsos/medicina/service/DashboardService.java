package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Rol;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.PacienteRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardService(
            TurnoRepository turnoRepository,
            PacienteRepository pacienteRepository,
            UsuarioRepository usuarioRepository) {

        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public ResumenDashboard obtenerResumen(String username) {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() ->
                        new AccessDeniedException("Usuario no encontrado"));

        boolean administrador =
                usuario.getRoles().contains(Rol.ROLE_ADMIN);

        boolean medico =
                usuario.getRoles().contains(Rol.ROLE_MEDICO);

        if (!usuario.isActivo() || (!administrador && !medico)) {
            throw new AccessDeniedException("Acceso no autorizado");
        }

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();

        List<Turno> turnos;

        if (administrador) {
            turnos = turnoRepository
                    .findByFechaHoraBetweenOrderByFechaHoraAsc(
                            inicio, fin.minusNanos(1));
        } else {
            turnos = turnoRepository
                    .findByMedicoIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                            usuario.getId(), inicio, fin);
        }

        long pendientes = contarEstado(turnos, "PENDIENTE");
        long enSalaDeEspera = contarEstado(turnos, "EN_SALA_DE_ESPERA");
        long atendidos = contarEstado(turnos, "ATENDIDO");
        long cancelados = contarEstado(turnos, "CANCELADO");

        // El total global de pacientes solo se expone a ADMIN.
        Long pacientesRegistrados = administrador
                ? pacienteRepository.count()
                : null;

        return new ResumenDashboard(
                usuario.getNombreCompleto(),
                administrador,
                hoy,
                turnos.size(),
                pendientes,
                enSalaDeEspera,
                atendidos,
                cancelados,
                pacientesRegistrados,
                turnos
        );
    }

    private long contarEstado(List<Turno> turnos, String estado) {
        return turnos.stream()
                .filter(t -> t.getEstado() != null)
                .filter(t -> estado.equalsIgnoreCase(
                        t.getEstado().trim()))
                .count();
    }

    public record ResumenDashboard(
            String nombreProfesional,
            boolean administrador,
            LocalDate fecha,
            long totalTurnos,
            long pendientes,
            long enSalaDeEspera,
            long atendidos,
            long cancelados,
            Long pacientesRegistrados,
            List<Turno> turnos
    ) {}
}
