package com.nyxeira.homerunner.entrymodel.model;

import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@DiscriminatorValue("EVENT")
@Entity
public class Event extends Entry {

    LocalDateTime endDate;

    @JoinTable(name = "event_participants")
    @ManyToMany
    Set<User> participants = new HashSet<>();

    public Event() {}

    public LocalDateTime getEndDate() { return endDate; }
    public Set<User> getParticipants() { return participants; }
    public void setParticipants(Set<User> participants) { this.participants = participants; }

    public EntryType getType() { return EntryType.EVENT; }

}
