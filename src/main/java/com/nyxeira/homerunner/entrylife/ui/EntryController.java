package com.nyxeira.homerunner.entrylife.ui;


import com.nyxeira.homerunner.common.web.WebPaths;
import com.nyxeira.homerunner.entrylife.services.EntryLifeService;
import com.nyxeira.homerunner.entrymodel.model.Entry;
import com.nyxeira.homerunner.entrymodel.model.EntryType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping(WebPaths.ENTRIES)
public class EntryController {

    private final EntryLifeService entryLifeService;
    private final String FORM_PATH = "entries/form";

    public EntryController(EntryLifeService entryLifeService) {
        this.entryLifeService = entryLifeService;
    }

    @GetMapping(WebPaths.ENTRY_NEW)
    public String getCreateForm(@RequestParam EntryType type, Model model) {
        CreateEntryForm form = new CreateEntryForm();
        form.setType(type);
        model.addAttribute("form", form);
        return FORM_PATH;
    }


    @PostMapping
    public String createEntry(@Valid @ModelAttribute("form") CreateEntryForm form, BindingResult bindingResult,
                              Principal principal) {

        if (bindingResult.hasErrors()) {
            return FORM_PATH;
        }
        Long id = switch (form.getType()) {
            case EVENT -> entryLifeService.createEvent(form.toEventDTO(), principal.getName());
            case TASK -> entryLifeService.createTask(form.toTaskDTO(), principal.getName());
        };
        return "redirect:/entries/" + id;
    }

    @GetMapping("/{id}")
    public String getEntry(@PathVariable Long id, Model model) {
        Entry entry = entryLifeService.findById(id);
        model.addAttribute("entry", entry);
        return "entries/detail";
    }

}
