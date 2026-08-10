package com.nyxeira.homerunner.entrymodel.model;

import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

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



}
