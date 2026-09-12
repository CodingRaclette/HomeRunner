package com.nyxeira.homerunner.notification.model;

import com.nyxeira.homerunner.entries.model.Entry;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rappel programmé, matérialisé en base pour survivre à un redémarrage du serveur
 * (cf. conception 4.8/5.5). ReminderScheduler balaie périodiquement les rappels dus.
 * <p>
 * Contrairement au reste du modèle technique (Entry.deletedAt...), fireAt est en LocalDateTime
 * et non en Instant : le projet n'a pas (encore) de fuseau d'instance configurable (cf. conception
 * 5.6), et Entry.date - dont fireAt dérive - est lui-même un LocalDateTime "naïf". Introduire une
 * conversion de fuseau ici seul aurait été trompeur sans configuration réelle derrière.
 */
@Entity
public class ScheduledReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    Entry entry;

    LocalDateTime fireAt;

    // Date de l'occurrence concernée ; null pour une entrée non récurrente. C'est cette date,
    // et non fireAt, qui identifie de façon stable le rappel d'une occurrence donnée (cf. 5.8).
    LocalDate occurrenceDate;

    boolean sent;

    public ScheduledReminder() {}

    public ScheduledReminder(Entry entry, LocalDateTime fireAt, LocalDate occurrenceDate) {
        this.entry = entry;
        this.fireAt = fireAt;
        this.occurrenceDate = occurrenceDate;
        this.sent = false;
    }

    public Long getId() { return id; }
    public Entry getEntry() { return entry; }
    public LocalDateTime getFireAt() { return fireAt; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public boolean isSent() { return sent; }

    public void markAsSent() { this.sent = true; }
}
