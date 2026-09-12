package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


/**
 * Cette entité définit un évènement de calendrier. L'évènement se distingue par le fait qu'il :
 *  - se déroule sur une plagetemporelle dont la fin est définie par endDate,
 */
@DiscriminatorValue("EVENT")
@Entity
public class Event extends Entry {

    LocalDateTime endDate;

    public Event() {}

    public Event(EventDTO d, User creator) {
        this.name = d.getName();
        this.date = d.getDate();
        this.description = d.getDescription();
        this.creator = creator;
        this.endDate = d.getEndDate();
        this.recurrence = d.toRecurrenceRule();
        this.reminderMinutesBefore = d.getReminderMinutesBefore();
    }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public EntryType getType() { return EntryType.EVENT; }

    public void applyData(EntryDTO dto) {
        EventDTO event = (EventDTO)dto;
        this.name = event.getName();
        this.date = event.getDate();
        this.description = event.getDescription();
        this.endDate = event.getEndDate();
        this.recurrence = event.toRecurrenceRule();
        this.reminderMinutesBefore = event.getReminderMinutesBefore();
    }
}
