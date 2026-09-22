package com.pulsos.medicina.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.pulsos.medicina.model.Turno;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;

@Service
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Pulsos Medicina & Bienestar";

    public void agregarTurnoACalendar(Turno turno, String googleAccessToken) {
        try {
            // Inicializar transporte y cliente de la API de Google
            com.google.api.client.http.HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            // Configurar credenciales usando el token OAuth del médico logueado
            com.google.auth.http.HttpCredentialsAdapter credentialAdapter = 
                new com.google.auth.http.HttpCredentialsAdapter(
                    new com.google.auth.oauth2.AccessToken(googleAccessToken, null)
                );

            Calendar service = new Calendar.Builder(
                    httpTransport, 
                    GsonFactory.getDefaultInstance(), 
                    credentialAdapter)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Construir el evento de Google Calendar
            Event event = new Event()
                    .setSummary("Turno: " + turno.getMotivo() + " - Paciente: " + turno.getPaciente().getNombreApellido())
                    .setDescription("Cita médica gestionada desde el sistema Pulsos.");

            // Configurar fecha y hora de inicio (ej. duración estimada de 30 minutos)
            DateTime startDateTime = new DateTime(turno.getFechaHora().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(startDateTime));

            DateTime endDateTime = new DateTime(turno.getFechaHora().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(endDateTime));

            // Insertar el evento en el calendario principal ("primary") del usuario
            String calendarId = "primary";
            service.events().insert(calendarId, event).execute();

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al sincronizar con Google Calendar: " + e.getMessage());
        }
    }
}
