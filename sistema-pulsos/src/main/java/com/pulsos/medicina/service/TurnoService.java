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
    private final UsuarioRepository usuarioRepo;
    private final GoogleCalendarService googleCalendarService;

    public TurnoService(TurnoRepository turnoRepo, 
                        PacienteRepository pacienteRepo, 
                        UsuarioRepository usuarioRepo, 
                        GoogleCalendarService googleCalendarService) {
        this.turnoRepo = turnoRepo;
        this.pacienteRepo = pacienteRepo;
        this.usuarioRepo = usuarioRepo;
        this.googleCalendarService = googleCalendarService;
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

        // 1. Guardar el turno en la base de datos local de forma segura
        Turno turnoGuardado = turnoRepo.save(turno);

        // 2. Intentar sincronización automática con Google Calendar según el usuario logueado
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String usernameLogueado = auth.getName();
                
                Usuario medicoLogueado = usuarioRepo.findByUsername(usernameLogueado).orElse(null);

                // Nota: Cuando agregues el campo de token en tu entidad Usuario, descomenta la siguiente línea:
                /*
                if (medicoLogueado != null && medicoLogueado.getGoogleAccessToken() != null) {
                    googleCalendarService.agregarTurnoACalendar(turnoGuardado, medicoLogueado.getGoogleAccessToken());
                }
                */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return turnoGuardado;
    }

    public void actualizarEstado(Long turnoId, String nuevoEstado) {
        Turno turno = turnoRepo.findById(turnoId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));
        turno.setEstado(nuevoEstado);
        turnoRepo.save(turno);
    }
}
