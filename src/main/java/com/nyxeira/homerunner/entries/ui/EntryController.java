package com.nyxeira.homerunner.entries.ui;

import com.nyxeira.homerunner.entries.dto.EntryFormDTO;
import com.nyxeira.homerunner.entries.services.EntryLifeService;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.RecurrenceRule;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.entries.model.occurrences.Occurrence;
import com.nyxeira.homerunner.entries.ui.tools.ParticipantOption;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/entries")
public class EntryController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    // Vue pour le template : la date brute sert aux liens/formulaires (retrieve, occurrence), le libellé formaté à l'affichage.
    public record ExDateView(LocalDate date, String formatted) {}

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

    @PostMapping("/{id}/occurrences/{date}/cancel")
    public String cancelOccurrence(@PathVariable Long id,
                                    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    Principal principal) {
        entryLifeService.cancelOccurrence(id, date, principal.getName());
        // l'occurrence annulée n'a plus de sens à afficher : on revient sur la master.
        return "redirect:/entries/" + id;
    }

    @PostMapping("/{id}/occurrences/{date}/retrieve")
    public String retrieveOccurrence(@PathVariable Long id,
                                      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      Principal principal) {
        entryLifeService.retrieveOccurrence(id, date, principal.getName());
        return "redirect:/entries/" + id + "?date=" + date;
    }

    @GetMapping("/{id}/occurrences/{date}/edit")
    public String getOccurrenceEditForm(@PathVariable Long id,
                                         @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         Principal principal, Model model, RedirectAttributes redirectAttributes) {
        try {
            EntryFormDTO form = entryLifeService.getEventOccurrenceForEdit(id, date, principal.getName());
            model.addAttribute("form", form);
            model.addAttribute("participantCandidates", getParticipantCandidates());
            // Indique au template form.html qu'il s'agit d'une occurrence isolée (et non de la master),
            // pour poster vers la bonne route et masquer les champs sans effet à ce niveau (type/récurrence/participants).
            model.addAttribute("occurrenceDate", date);
            return FORM_PATH;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas modifier cette occurrence.");
            return redirectToEntry(id, date);
        }
    }

    @PostMapping("/{id}/occurrences/{date}/edit")
    public String updateOccurrence(@Valid @ModelAttribute("form") EntryFormDTO form, BindingResult bindingResult,
                                    @PathVariable Long id,
                                    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("participantCandidates", getParticipantCandidates());
            model.addAttribute("occurrenceDate", date);
            return FORM_PATH;
        }
        entryLifeService.updateEventOccurrence(id, date, form.toEventDTO(), principal.getName());
        return "redirect:/entries/" + id + "?date=" + date;
    }

    @GetMapping("/{id}")
    public String getEntry(@PathVariable Long id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model, Principal principal) {
        Entry entry = entryLifeService.findById(id);
        // date n'a de sens que pour une entrée récurrente : sinon on retombe sur l'affichage de la master.
        boolean dateApplies = date != null && entry.isRecurring();
        Occurrence occurrence = dateApplies ? entryLifeService.resolveOccurrence(entry, date) : null;
        model.addAttribute("entry", entry);
        model.addAttribute("occurrence", occurrence);
        model.addAttribute("occurrenceDate", dateApplies ? date : null);

        // Utilise par le bloc TASK de entries/detail.html pour savoir s'il faut proposer
        // "S'assigner" ou "Se désassigner" a l'utilisateur courant.
        User currentUser = userRepository.findByLogin(principal.getName()).orElseThrow();
        Set<User> participants = occurrence != null ? occurrence.getParticipants() : entry.getParticipants();
        model.addAttribute("currentUserAssigned", participants.contains(currentUser));

        // Contrôle l'affichage des boutons +/retirer sur la liste de participants : seul quelqu'un
        // pouvant modifier l'entrée (isEditableBy) peut gérer les participants d'un tiers.
        boolean canEdit = entry.isEditableBy(currentUser);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("canDelete", entry.isDeletableBy(currentUser));
        if (canEdit) {
            List<Long> participantIds = participants.stream().map(User::getId).toList();
            model.addAttribute("availableParticipants", getParticipantCandidates().stream()
                    .filter(p -> !participantIds.contains(p.id()))
                    .toList());
        }

        boolean allDay = isAllDay(entry);
        model.addAttribute("allDay", allDay);
        // Seule la date d'ancrage (heure comprise) est reportée sur l'occurrence : la récurrence ne
        // permet pas de decaler l'heure d'une occurrence individuelle, seulement son contenu.
        LocalDateTime displayDate = occurrence != null ? date.atTime(entry.getDate().toLocalTime()) : entry.getDate();
        model.addAttribute("formattedDate", allDay ? displayDate.format(DATE_FMT) : displayDate.format(DATETIME_FMT));
        if (entry instanceof Event event && !allDay) {
            LocalDateTime endDate = occurrence instanceof EventOccurrence eventOccurrence ? eventOccurrence.getEndDate() : event.getEndDate();
            model.addAttribute("formattedEndDate", endDate.format(DATETIME_FMT));
        }

        boolean hasRecurrence = entry.isRecurring() && entry.getRecurrence().getFrequency() != null;
        model.addAttribute("hasRecurrence", hasRecurrence);
        if (hasRecurrence) {
            RecurrenceRule recurrence = entry.getRecurrence();
            model.addAttribute("recurrenceSummary", formatRecurrenceSummary(recurrence));
            if (recurrence.getUntil() != null) {
                model.addAttribute("recurrenceUntil", recurrence.getUntil().format(DATE_FMT));
            }
            model.addAttribute("recurrenceExDates", recurrence.getExDates().stream().sorted()
                    .map(d -> new ExDateView(d, DATE_FMT.format(d))).toList());
        }
        return "entries/detail";
    }


    @PostMapping("/{id}/participants/add")
    public String addParticipant(@PathVariable Long id, @RequestParam Long targetUserId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Principal principal, RedirectAttributes redirectAttributes) {
        try {
            entryLifeService.addParticipant(id, targetUserId, date, principal.getName());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas modifier les participants de cette entrée.");
        }
        return redirectToEntry(id, date);
    }

    @PostMapping("/{id}/participants/remove")
    public String removeParticipant(@PathVariable Long id, @RequestParam Long targetUserId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     Principal principal, RedirectAttributes redirectAttributes) {
        try {
            entryLifeService.removeParticipant(id, targetUserId, date, principal.getName());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas modifier les participants de cette entrée.");
        }
        return redirectToEntry(id, date);
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

    // Reconstruit l'URL de retour en conservant le contexte d'occurrence (date) quand il y en a un,
    // pour que l'utilisateur retombe sur la même vue qu'avant l'action (cf. TaskTrackingController).
    private String redirectToEntry(Long id, LocalDate date) {
        return "redirect:/entries/" + id + (date != null ? "?date=" + date : "");
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
