package com.nyxeira.homerunner.entries.ui;

import com.nyxeira.homerunner.entries.exceptions.UserNotParticipantException;
import com.nyxeira.homerunner.entries.services.TaskTrackingService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/entries")
public class TaskTrackingController {

    private final TaskTrackingService taskTrackingService;

    public TaskTrackingController(TaskTrackingService taskTrackingService) {
        this.taskTrackingService = taskTrackingService;
    }

    @PostMapping("/{id}/self-assign")
    public String selfAssign(@PathVariable Long id, Principal principal) {
        taskTrackingService.selfAssign(id, principal.getName());
        return "redirect:/entries/" + id;
    }

    @PostMapping("/{id}/self-unassign")
    public String selfUnassign(@PathVariable Long id, Principal principal) {
        taskTrackingService.selfUnassign(id, principal.getName());
        return "redirect:/entries/" + id;
    }

    @PostMapping("/{id}/toggle-contrib")
    public String toggleContrib(@PathVariable Long id, Long targetUserId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            taskTrackingService.toggleContributor(id, targetUserId, principal.getName());
        } catch (UserNotParticipantException e) {
            redirectAttributes.addFlashAttribute("message", "L'utilisateur ne participe pas à cette tâche");
            return "redirect:/entries/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("message", "L'utilisateur n'est pas autorisé à réaliser cette action");
            return "redirect:/entries/" + id;
        }
            return "redirect:/entries/" + id;
    }

    @PostMapping("/{id}/toggle-validated-by-other")
    public String toggleValidatedByOther(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            taskTrackingService.toggleValidatedByOther(id, principal.getName());
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("message", "L'utilisateur ne peut pas faire cette action");
            return "redirect:/entries/" + id;
        }

        return "redirect:/entries/" + id;
    }
}
