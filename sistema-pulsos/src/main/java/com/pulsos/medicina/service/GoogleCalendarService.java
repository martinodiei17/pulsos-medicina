package com.pulsos.medicina.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.pulsos.medicina.model.Turno;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;

@Service
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Pulsos Medicina & Bienestar";

    public void agregarTurnoACalendar(Turno turno, String googleAccessToken) {
        try {
            com.google.api.client.http.HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            HttpCredentialsAdapter credentialAdapter = new HttpCredentialsAdapter(
                new AccessToken(googleAccessToken, null)
            );

            Calendar service = new Calendar.Builder(
                    httpTransport, 
                    GsonFactory.getDefaultInstance(), 
                    credentialAdapter)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Usamos getNombre() y getApellido() asumiendo los métodos estándar del modelo Paciente
            String nombrePaciente = turno.getPaciente() != null ? turno.getPaciente.getNombre() + " " + turno.getPaciente.getApellido() : "Sin paciente";

            Event event = new Event()
                    .setSummary("Turno: " + turno.getMotivo() + " - Paciente: " + nombrePaciente)
                    .setDescription("Cita médica gestionada desde el sistema Pulsos.");

            DateTime startDateTime = new DateTime(turno.getFechaHora().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(startDateTime));

            DateTime endDateTime = new DateTime(turno.getFechaHora().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(endDateTime));

            service.events().insert("primary", event).execute();

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al sincronizar con Google Calendar: " + e.getMessage());
        }
    }
}
