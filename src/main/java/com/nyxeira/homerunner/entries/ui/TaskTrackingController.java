package com.nyxeira.homerunner.entries.ui;

import com.nyxeira.homerunner.entries.services.TaskTrackingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

}
