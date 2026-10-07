package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.PacienteRepository;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepo;
    private final PacienteRepository pacienteRepo;
    private final UsuarioRepository usuarioRepo; // Añadido para buscar al usuario logueado

    public TurnoService(TurnoRepository turnoRepo, PacienteRepository pacienteRepo, UsuarioRepository usuarioRepo) {
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
        return turnoRepo.findByFechaHoraBetweenOrderByFechaHoraAsc(start, end);
    }

    public Turno agendarTurno(Long pacienteId, Turno turno) {
        Paciente paciente = pacienteRepo.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        turno.setPaciente(paciente);

        // 1. Obtenemos el usuario autenticado actual desde el contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String usernameOrEmail = auth.getName();
            
            // 2. Buscamos al usuario en la base de datos (dependiendo de si usás email o username)
            Usuario usuarioLogueado = usuarioRepo.findByEmail(usernameOrEmail)
                    .orElseGet(() -> usuarioRepo.findByUsername(usernameOrEmail).orElse(null));

            // 3. Si tu entidad Turno tiene un campo para relacionarlo al usuario/medico, lo asignás acá:
            // Por ejemplo: turno.setUsuario(usuarioLogueado); o turno.setMedico(usuarioLogueado);
            if (usuarioLogueado != null) {
                // Descomentá y ajustá el método según cómo se llame en tu entidad Turno.java (ej: setUsuario / setMedico)
                // turno.setUsuario(usuarioLogueado);
            }
        }

        return turnoRepo.save(turno);
    }

    public void actualizarEstado(Long turnoId, String nuevoEstado) {
        Turno turno = turnoRepo.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));
        turno.setEstado(nuevoEstado);
        turnoRepo.save(turno);
    }
}
