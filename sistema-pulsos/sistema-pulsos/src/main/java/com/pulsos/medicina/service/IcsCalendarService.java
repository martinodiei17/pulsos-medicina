package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Turno;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.TurnoRepository;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class IcsCalendarService {

    private final TurnoRepository turnoRepo;
    private final UsuarioRepository usuarioRepo;
    private static final DateTimeFormatter ICS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public IcsCalendarService(TurnoRepository turnoRepo, UsuarioRepository usuarioRepo) {
        this.turnoRepo = turnoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public String generarFeedParaMedico(String identificador) {
        Usuario medico = usuarioRepo.findByTokenCalendario(identificador)
                .or(() -> usuarioRepo.findByUsername(identificador))
                .or(() -> usuarioRepo.findByNombreCompleto(identificador))
                .orElse(null);

        String nombreBuscado = (medico != null) ? medico.getNombreCompleto() : identificador;

        List<Turno> turnos = turnoRepo.findAllByOrderByFechaHoraDesc().stream()
                .filter(t -> t.getMedicoAsignado() != null && t.getMedicoAsignado().equalsIgnoreCase(nombreBuscado))
                .filter(t -> !"CANCELADO".equalsIgnoreCase(t.getEstado()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Pulsos Medicina & Bienestar//Agenda Medica//ES\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:Pulsos - ").append(nombreBuscado).append("\r\n");
        sb.append("X-WR-TIMEZONE:America/Argentina/Buenos_Aires\r\n");

        for (Turno t : turnos) {
            LocalDateTime inicio = t.getFechaHora();
            LocalDateTime fin = inicio.plusMinutes(45);

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:turno-").append(t.getId()).append("@pulsosmedicina.com.ar\r\n");
            sb.append("DTSTAMP:").append(LocalDateTime.now().format(ICS_DATE_FORMAT)).append("\r\n");
            sb.append("DTSTART:").append(inicio.format(ICS_DATE_FORMAT)).append("\r\n");
            sb.append("DTEND:").append(fin.format(ICS_DATE_FORMAT)).append("\r\n");
            sb.append("SUMMARY:Paciente: ").append(t.getPaciente().getNombreCompleto()).append("\r\n");

            String desc = "Paciente: " + t.getPaciente().getNombreCompleto() +
                    "\\nDNI: " + t.getPaciente().getDni() +
                    "\\nTel: " + (t.getPaciente().getTelefono() != null ? t.getPaciente().getTelefono() : "S/D") +
                    "\\nCobertura: " + (t.getPaciente().getObraSocial() != null ? t.getPaciente().getObraSocial() : "Particular") +
                    "\\nMotivo: " + (t.getMotivo() != null ? t.getMotivo() : "Consulta") +
                    "\\nEstado: " + t.getEstado();

            sb.append("DESCRIPTION:").append(desc).append("\r\n");
            sb.append("LOCATION:Centro Medico Pulsos\\, Buenos Aires\r\n");
            sb.append("STATUS:CONFIRMED\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }
}
