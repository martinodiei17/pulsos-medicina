package com.pulsos.medicina.controller;

import com.pulsos.medicina.service.IcsCalendarService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendario")
public class CalendarSyncController {

    private final IcsCalendarService icsService;

    public CalendarSyncController(IcsCalendarService icsService) {
        this.icsService = icsService;
    }

    @GetMapping(value = "/medico/{identificador}/turnos.ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<String> descargarFeedIcs(@PathVariable String identificador) {
        String icsContent = icsService.generarFeedParaMedico(identificador);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"agenda_pulsos_" + identificador + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(icsContent);
    }
}
