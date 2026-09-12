package com.nyxeira.homerunner.entries.dto;


import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.model.RecurrenceRule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public abstract class EntryDTO {

    private String name;
    private LocalDateTime date;
    private String description;
    private List<Long> participantIds;
    private Frequency frequency;
    private int interval;
    private LocalDate until;
    private Integer reminderMinutesBefore;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds) {
        this.participantIds = participantIds;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public LocalDate getUntil() {
        return until;
    }

    public void setUntil(LocalDate until) {
        this.until = until;
    }

    public Integer getReminderMinutesBefore() {
        return reminderMinutesBefore;
    }

    public void setReminderMinutesBefore(Integer reminderMinutesBefore) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }

    // frequency == null signifie "pas de recurrence" ; c'est le seul champ qui fait foi.
    public RecurrenceRule toRecurrenceRule() {
        return frequency == null ? null : new RecurrenceRule(frequency, interval, until);
    }
}
