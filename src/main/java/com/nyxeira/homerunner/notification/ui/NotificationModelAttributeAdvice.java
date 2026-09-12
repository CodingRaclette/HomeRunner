package com.nyxeira.homerunner.notification.ui;

import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Injecte le compteur de notifications non lues et les plus récentes dans le modèle de chaque
 * page rendue, pour alimenter la cloche de fragments/layout.html::navbar sans que chaque
 * contrôleur ait à s'en soucier.
 */
@ControllerAdvice
public class NotificationModelAttributeAdvice {

    private static final int DROPDOWN_SIZE = 5;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationModelAttributeAdvice(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addNotificationSummary(Principal principal, Model model) {
        if (principal == null) return;
        userRepository.findByLogin(principal.getName()).ifPresent(user -> {
            model.addAttribute("unreadNotificationCount", notificationRepository.countByRecipientAndReadAtIsNull(user));
            model.addAttribute("recentNotifications", notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                    .stream().limit(DROPDOWN_SIZE).map(NotificationView::from).toList());
        });
    }
}
