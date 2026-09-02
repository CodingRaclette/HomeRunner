package com.nyxeira.homerunner.calendarview.ui;

import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalendarController {
    // raccourci temporaire : accès direct au repo depuis un contrôleur ; sera remplacé par CalendarService un peu plus tard
    private final EntryRepository entryRepository;

    public CalendarController(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @GetMapping("/")
    public String list(Model model) {
        model.addAttribute("entries", entryRepository.findAll());
        return "entries/list";
    }
}
