package com.nyxeira.homerunner.entries.model.occurrences;

import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@MappedSuperclass
public abstract class AbstractOccurrence implements Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToMany
    Set<User> participants;
    boolean participantsOverridden = false;

    LocalDate date;
    String name;
    String description;

    public void applyOverrides(EntryDTO d) {
        this.name = d.getName();
        this.description = d.getDescription();
    }

    public void overrideParticipants(Set<User> p) {
        this.participants = p;
        this.participantsOverridden = true;
    }

    @Override
    public String getName() {
        return name != null ? name : this.getMaster().getName();
    }

    @Override
    public String getDescription() {
        return description != null ? description : this.getMaster().getDescription();
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public Set<User> getParticipants() {
        return this.participantsOverridden ? this.participants : getMaster().getParticipants();
    }

    @Override
    public boolean isParticipant(User user) {
        return getParticipants().contains(user);
    }

    private void ensureOverride() {
        if (!participantsOverridden) {
            overrideParticipants(new HashSet<>(getMaster().getParticipants()));
        }
    }

    @Override
    public void addParticipant(User user) {
        ensureOverride();
        this.participants.add(user);
    }

    @Override
    public void removeParticipant(User user) {
        ensureOverride();
        this.participants.remove(user);
    }

}
