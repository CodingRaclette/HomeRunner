package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

/**
 * Entrée de calendrier (évènement ou tâche, pour l'instant, mais est amené à recevoir d'autre types d'entrées)
 * Mappé en single table pour que les requêtes restent polymorphes sans jointure.
 */
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "entry_type")
@SQLRestriction("deleted_at is null")
@Entity
public abstract class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    LocalDateTime date;

    String description;

    Instant deletedAt;

    @ManyToOne
    User creator;

    @JoinTable(name = "entry_participants")
    @ManyToMany
    Set<User> participants = new HashSet<>();

    @Embedded
    RecurrenceRule recurrence;

    // null = pas de rappel demandé. Sinon, nombre de minutes avant l'échéance auquel notifier
    // les participants (cf. notification.services.NotificationService / ReminderScheduler).
    Integer reminderMinutesBefore;

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getDate() { return date; }
    public String getDescription() { return description; }


    public Instant getDeletedAt() { return deletedAt; }
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public User getCreator() { return creator; }
    public RecurrenceRule getRecurrence() { return recurrence; }
    public boolean isRecurring() {
        // Hibernate matérialise toujours un RecurrenceRule non-null au chargement (a cause du
        // @ElementCollection exDates dans l'embeddable), meme quand aucune recurrence n'a ete
        // definie : recurrence != null ne suffit donc pas. frequency == null fait foi (cf.
        // EntryDTO.toRecurrenceRule) pour determiner si l'entree est reellement recurrente.
        return recurrence != null && recurrence.getFrequency() != null;
    }

    public Integer getReminderMinutesBefore() { return reminderMinutesBefore; }

    public abstract EntryType getType();


    public Set<User> getParticipants() {return participants;}
    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }
    public boolean isParticipant(User user) { return participants.contains(user); }
    public void addParticipant(User user) { participants.add(user); }
    public void removeParticipant(User user) { participants.remove(user); }

    public List<Long> getParticipantIds() {
        return getParticipants().stream().map(User::getId).toList();
    }

    public boolean isEditableBy(User user) {
        return user.equals(creator);
    }

    public boolean isDeletableBy(User user) {
        return user.equals(creator) || user.getRole() == UserRole.ADMIN;
    }

    public abstract void applyData(EntryDTO dto);



}
