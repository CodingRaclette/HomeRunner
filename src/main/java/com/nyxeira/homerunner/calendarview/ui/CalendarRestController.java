package com.nyxeira.homerunner.calendarview.ui;

import com.nyxeira.homerunner.entries.dto.CalendarItemDTO;
import com.nyxeira.homerunner.entries.services.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Expose le calendrier en JSON pour FullCalendar (cote client).
 * start/end sont les bornes de la periode visible, en LocalDate (jour entier) ;
 * on les convertit en LocalDateTime avant de deleguer a CalendarService.
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarRestController {

    private final CalendarService calendarService;

    public CalendarRestController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public List<CalendarItemDTO> getCalendar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return calendarService.getCalendar(start.atStartOfDay(), end.atTime(LocalTime.MAX));
    }
}
