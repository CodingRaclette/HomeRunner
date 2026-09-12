package com.nyxeira.homerunner.notification.ui;

import com.nyxeira.homerunner.notification.model.Notification;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Vue d'affichage d'une Notification : Notification.createdAt est un Instant, non directement
 * formatable par un pattern "jour/mois/heure" (cf. Thymeleaf #temporals) sans passer par un
 * fuseau. On construit donc la chaîne déjà formatée ici, dans le même esprit que
 * EntryController.ExDateView.
 */
public record NotificationView(Long id, String message, String link, boolean read, String formattedDate) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    public static NotificationView from(Notification n) {
        return new NotificationView(n.getId(), n.getMessage(), n.getLink(), n.isRead(), FORMATTER.format(n.getCreatedAt()));
    }
}
