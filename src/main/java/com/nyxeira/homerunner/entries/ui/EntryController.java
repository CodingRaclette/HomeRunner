package com.nyxeira.homerunner.entries.ui;

import com.nyxeira.homerunner.entries.dto.EntryFormDTO;
import com.nyxeira.homerunner.entries.services.EntryLifeService;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.RecurrenceRule;
import com.nyxeira.homerunner.entries.ui.tools.ParticipantOption;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/entries")
public class EntryController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

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

        // Définition de la date par défaut à la date actuelle, et h+1 pour la date de fin si event
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        form.setDate(now);
        if (type == EntryType.EVENT) {
            form.setEndDate(now.plusHours(1));
        }

        model.addAttribute("form", form);
        model.addAttribute("participantCandidates", getParticipantCandidates());
        return FORM_PATH;
    }


    @PostMapping
    public String createEntry(@Valid @ModelAttribute("form") EntryFormDTO form, BindingResult bindingResult,
                              Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            // sans ca, participant-picker.js ne retrouve plus aucun candidat pour reconstruire les tags
            // et les champs caches deja selectionnes : la liste des participants semble effacee.
            model.addAttribute("participantCandidates", getParticipantCandidates());
            return FORM_PATH;
        }
        Long id = switch (form.getType()) {
            case EVENT -> entryLifeService.createEvent(form.toEventDTO(), principal.getName());
            case TASK -> entryLifeService.createTask(form.toTaskDTO(), principal.getName());
        };
        return "redirect:/entries/" + id;
    }

    @GetMapping("/{id}/edit")
    public String getEditForm(@RequestParam(required = false, defaultValue = "EVENT") EntryType type, Principal principal, @PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            EntryFormDTO form = entryLifeService.getEntryForEdit(id, principal.getName());
            model.addAttribute("form", form);
            model.addAttribute("participantCandidates", getParticipantCandidates());
            return FORM_PATH;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas modifier cette entrée.");
            return "redirect:/entries/" + id;
        }

    }

    @PostMapping("/{id}/edit")
    public String updateEntry(@Valid @ModelAttribute("form") EntryFormDTO form, BindingResult bindingResult, @PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            // meme correctif que createEntry : sans participantCandidates ici, le picker perd la
            // selection deja faite au moment de reafficher le formulaire avec les erreurs.
            model.addAttribute("participantCandidates", getParticipantCandidates());
            return FORM_PATH;
        }
        switch (form.getType()) {
            case EVENT -> entryLifeService.updateEntry(id, form.toEventDTO(), principal.getName());
            case TASK -> entryLifeService.updateEntry(id, form.toTaskDTO(), principal.getName());
        }

        return "redirect:/entries/" + id;
    }

    @GetMapping("/{id}")
    public String getEntry(@PathVariable Long id, Model model, Principal principal) {
        Entry entry = entryLifeService.findById(id);
        model.addAttribute("entry", entry);
        // Utilise par le bloc TASK de entries/detail.html pour savoir s'il faut proposer
        // "S'assigner" ou "Se désassigner" a l'utilisateur courant.
        User currentUser = userRepository.findByLogin(principal.getName()).orElseThrow();
        model.addAttribute("currentUserAssigned", entry.getParticipants().contains(currentUser));

        boolean allDay = isAllDay(entry);
        model.addAttribute("allDay", allDay);
        model.addAttribute("formattedDate", allDay ? entry.getDate().format(DATE_FMT) : entry.getDate().format(DATETIME_FMT));
        if (entry instanceof Event event && !allDay) {
            model.addAttribute("formattedEndDate", event.getEndDate().format(DATETIME_FMT));
        }

        // entry.isRecurring() ne garantit que recurrence != null : sur certaines entrees
        // (donnees de test anterieures a la validation actuelle, ou formulaire soumis avec le
        // toggle "Recurrent" decoche sans que interval/until aient ete videes cote client),
        // l'objet existe mais frequency est null. On le traite alors comme "pas de recurrence"
        // plutot que de planter sur le switch de formatRecurrenceSummary.
        boolean hasRecurrence = entry.isRecurring() && entry.getRecurrence().getFrequency() != null;
        model.addAttribute("hasRecurrence", hasRecurrence);
        if (hasRecurrence) {
            RecurrenceRule recurrence = entry.getRecurrence();
            model.addAttribute("recurrenceSummary", formatRecurrenceSummary(recurrence));
            if (recurrence.getUntil() != null) {
                model.addAttribute("recurrenceUntil", recurrence.getUntil().format(DATE_FMT));
            }
            model.addAttribute("recurrenceExDates", recurrence.getExDates().stream().sorted().map(DATE_FMT::format).toList());
        }

        return "entries/detail";
    }


    @PostMapping("/{id}/delete")
    public String deleteEntry(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            entryLifeService.deleteEntry(id, principal.getName());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas supprimer cette entrée.");
            return "redirect:/entries/" + id;
        }
        redirectAttributes.addFlashAttribute("successMessage", "L'entrée " + id + " a bien été supprimée");
        return "redirect:/";
    }

    // Le compte ADMIN est réservé a l'administration du systeme : il ne doit jamais pouvoir apparaitre comme participant d'un evenement.
    private List<ParticipantOption> getParticipantCandidates() {
        return userRepository.findAllByRoleNot(UserRole.ADMIN).stream()
                .map(u -> new ParticipantOption(u.getId(), u.getName(), u.getLogin()))
                .toList();
    }

    // "Toute la journee" n'est pas un flag explicite en base : on le deduit du meme motif que
    // celui construit cote client par le toggle "Toute la journee" du formulaire (00h00 -> 23h59
    // le meme jour pour un Event ; 00h00 seul pour une Task, qui n'a pas de date de fin).
    private boolean isAllDay(Entry entry) {
        LocalDateTime date = entry.getDate();
        if (date.getHour() != 0 || date.getMinute() != 0) {
            return false;
        }
        if (entry instanceof Event event) {
            LocalDateTime end = event.getEndDate();
            return end != null && end.getHour() == 23 && end.getMinute() == 59;
        }
        return true;
    }

    private String formatRecurrenceSummary(RecurrenceRule recurrence) {
        int n = recurrence.getInterval() == null ? 1 : recurrence.getInterval();
        return switch (recurrence.getFrequency()) {
            case DAILY -> n <= 1 ? "Tous les jours" : "Tous les " + n + " jours";
            case WEEKLY -> n <= 1 ? "Toutes les semaines" : "Toutes les " + n + " semaines";
            case MONTHLY -> n <= 1 ? "Tous les mois" : "Tous les " + n + " mois";
            case YEARLY -> n <= 1 ? "Tous les ans" : "Tous les " + n + " ans";
        };
    }

}
