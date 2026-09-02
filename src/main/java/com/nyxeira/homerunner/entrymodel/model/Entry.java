package com.nyxeira.homerunner.entrymodel.model;

import com.nyxeira.homerunner.entrylife.dto.EntryDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

/**
 * Entrée de calendrier (évènement ou tâche, pour l'instant, mais est amené à recevoir d'autre types d'entrées)
 * Mappé en single table pour que les requêtes restent polymorphes sans jointure.
 */
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "entry_type")
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

    @Embedded
    RecurrenceRule recurrence;

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getDate() { return date; }
    public String getDescription() { return description; }
    public Instant getDeletedAt() { return deletedAt; }
    public User getCreator() { return creator; }
    public RecurrenceRule getRecurrence() { return recurrence; }

    public abstract EntryType getType();

    // Je l'ajoute à ce niveau pour l'instant, mais cela pourrait poser question lors de l'ajout de nouvelles entrées
    // qui n'auraient pas de participants
    public abstract Set<User> getParticipants();

    public boolean isEditableBy(User user) {
        return user.equals(creator);
    }

    public boolean isDeletableBy(User user) {
        return user.equals(creator) || user.getRole() == UserRole.ADMIN;
    }

    public abstract void applyData(EntryDTO dto);


}
