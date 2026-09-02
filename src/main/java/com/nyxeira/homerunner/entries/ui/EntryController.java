package com.nyxeira.homerunner.entries.ui;

import com.nyxeira.homerunner.entries.dto.EntryFormDTO;
import com.nyxeira.homerunner.entries.services.EntryLifeService;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/entries")
public class EntryController {

    private final EntryLifeService entryLifeService;
    private final UserRepository userRepository;
    private final String FORM_PATH = "entries/form";

    public EntryController(EntryLifeService entryLifeService, UserRepository userRepository) {
        this.entryLifeService = entryLifeService;
        this.userRepository = userRepository;
    }

    // "type" n'est plus qu'une pre-selection : le formulaire permet desormais de basculer
    // dynamiquement (cote client) entre EVENT et TASK sans recharger la page.
    @GetMapping("/new")
    public String getCreateForm(@RequestParam(required = false, defaultValue = "EVENT") EntryType type, Model model) {
        EntryFormDTO form = new EntryFormDTO();
        form.setType(type);
        model.addAttribute("form", form);
        model.addAttribute("participantCandidates", getParticipantCandidates());
        return FORM_PATH;
    }


    @PostMapping
    public String createEntry(@Valid @ModelAttribute("form") EntryFormDTO form, BindingResult bindingResult,
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

    @GetMapping("/{id}/edit")
    public String getEditForm(@RequestParam(required = false, defaultValue = "EVENT") EntryType type, Principal principal, @PathVariable Long id, Model model) {
        EntryFormDTO form = entryLifeService.getEntryForEdit(id, principal.getName());
        model.addAttribute("form", form);
        model.addAttribute("participantCandidates", getParticipantCandidates());
        return FORM_PATH;
    }

    @PostMapping("/{id}/edit")
    public String updateEntry(@Valid @ModelAttribute("form") EntryFormDTO form, BindingResult bindingResult, @PathVariable Long id, Principal principal) {
        if (bindingResult.hasErrors()) { return FORM_PATH;}
        switch (form.getType()) {
            case EVENT -> entryLifeService.updateEntry(id, form.toEventDTO(), principal.getName());
            case TASK -> entryLifeService.updateEntry(id, form.toTaskDTO(), principal.getName());
        }
        return "redirect:/entries/" + id;
    }

    @GetMapping("/{id}")
    public String getEntry(@PathVariable Long id, Model model) {
        Entry entry = entryLifeService.findById(id);
        model.addAttribute("entry", entry);
        return "entries/detail";
    }

    // Le compte ADMIN est réservé a l'administration du systeme : il ne doit jamais pouvoir apparaitre comme participant d'un evenement.
    private List<ParticipantOption> getParticipantCandidates() {
        return userRepository.findAllByRoleNot(UserRole.ADMIN).stream()
                .map(u -> new ParticipantOption(u.getId(), u.getName(), u.getLogin()))
                .toList();
    }

}
