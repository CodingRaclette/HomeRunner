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
        return recurrence != null;
    }

    public abstract EntryType getType();


    public Set<User> getParticipants() {return participants;}
    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

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
