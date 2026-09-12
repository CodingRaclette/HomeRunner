package com.nyxeira.homerunner.notification.ui;

import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        User user = userRepository.findByLogin(principal.getName()).orElseThrow();
        model.addAttribute("notifications", notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream().map(NotificationView::from).toList());
        return "notifications/list";
    }

    // Suit le lien d'une notification en la marquant lue au passage, pour éviter un aller-retour manuel.
    @GetMapping("/{id}/open")
    public String open(@PathVariable Long id, Principal principal) {
        User user = userRepository.findByLogin(principal.getName()).orElseThrow();
        return notificationRepository.findById(id)
                .filter(n -> n.getRecipient().equals(user))
                .map(n -> {
                    n.markAsRead();
                    notificationRepository.save(n);
                    return "redirect:" + (n.getLink() != null ? n.getLink() : "/notifications");
                })
                .orElse("redirect:/notifications");
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Principal principal) {
        User user = userRepository.findByLogin(principal.getName()).orElseThrow();
        notificationRepository.findByRecipientOrderByCreatedAtDesc(user).forEach(n -> {
            n.markAsRead();
            notificationRepository.save(n);
        });
        return "redirect:/notifications";
    }
}
