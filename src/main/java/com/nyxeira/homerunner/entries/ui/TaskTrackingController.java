package com.nyxeira.homerunner.entries.ui;

import com.nyxeira.homerunner.entries.exceptions.UserNotParticipantException;
import com.nyxeira.homerunner.entries.services.TaskTrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/entries")
public class TaskTrackingController {

    private final TaskTrackingService taskTrackingService;

    public TaskTrackingController(TaskTrackingService taskTrackingService) {
        this.taskTrackingService = taskTrackingService;
    }

    @PostMapping("/{id}/self-assign")
    public String selfAssign(@PathVariable Long id,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Principal principal) {
        taskTrackingService.selfAssign(id, date, principal.getName());
        return redirect(id, date);
    }

    @PostMapping("/{id}/self-unassign")
    public String selfUnassign(@PathVariable Long id,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                Principal principal) {
        taskTrackingService.selfUnassign(id, date, principal.getName());
        return redirect(id, date);
    }

    @PostMapping("/{id}/toggle-contrib")
    public String toggleContrib(@PathVariable Long id, Long targetUserId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        try {
            taskTrackingService.toggleContributor(id, date, targetUserId, principal.getName());
        } catch (UserNotParticipantException e) {
            redirectAttributes.addFlashAttribute("message", "L'utilisateur ne participe pas à cette tâche");
            return redirect(id, date);
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("message", "L'utilisateur n'est pas autorisé à réaliser cette action");
            return redirect(id, date);
        }
            return redirect(id, date);
    }

    @PostMapping("/{id}/toggle-validated-by-other")
    public String toggleValidatedByOther(@PathVariable Long id,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          Principal principal, RedirectAttributes redirectAttributes) {
        try {
            taskTrackingService.toggleValidatedByOther(id, date, principal.getName());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("message", "L'utilisateur ne peut pas faire cette action");
            return redirect(id, date);
        }

        return redirect(id, date);
    }

    // Reconstruit l'URL de retour en conservant le contexte d'occurrence (date) quand il y en a un,
    // pour que l'utilisateur retombe sur la même vue qu'avant l'action.
    private String redirect(Long id, LocalDate date) {
        return "redirect:/entries/" + id + (date != null ? "?date=" + date : "");
    }
}
