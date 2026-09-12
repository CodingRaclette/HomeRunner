package com.nyxeira.homerunner.notification.model;

import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Notification in-app persistée en base (cf. conception 4.8 et 5.5) : NotificationService en
 * est l'unique producteur, en réaction aux événements applicatifs.
 */
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    User recipient;

    String message;

    // Chemin relatif vers l'entrée concernée (ex. "/entries/5"), suivi lors de la consultation.
    String link;

    Instant createdAt;

    Instant readAt;

    public Notification() {}

    public Notification(User recipient, String message, String link) {
        this.recipient = recipient;
        this.message = message;
        this.link = link;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    public boolean isRead() { return readAt != null; }

    public void markAsRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
