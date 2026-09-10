package com.nyxeira.homerunner.entries.model;


import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Règle de récurrence : sous-ensemble simplifié de la RFC 5545.
 * Les occurrences ne sont pas pré-générées en base ; elles sont calculées à la demande.
 */
@Embeddable
public class RecurrenceRule {

    @Enumerated(EnumType.STRING)
    Frequency frequency; // Basée sur un enum non développé explicitement lors de la conception

    @Column(name = "recurrence_interval") // "interval" est un mot réservé SQL (H2)
    Integer interval;

    LocalDate until;

    @ElementCollection
    Set<LocalDate> exDates = new HashSet<>();

    public RecurrenceRule() {}

    public RecurrenceRule(Frequency frequency, Integer interval, LocalDate until) {
        this.frequency = frequency;
        this.interval = interval;
        this.until = until;
        this.exDates = new HashSet<>();
    }

    public Frequency getFrequency() { return frequency; }
    public Integer getInterval() { return interval; }
    public LocalDate getUntil() { return until; }
    public Set<LocalDate> getExDates() { return exDates; }

    private LocalDateTime getNextDate(LocalDateTime currentDate) {
        return switch (frequency) {
            case DAILY -> currentDate.plusDays(interval);
            case WEEKLY -> currentDate.plusWeeks(interval);
            case MONTHLY -> currentDate.plusMonths(interval);
            case YEARLY -> currentDate.plusYears(interval);
        };
    }

    public List<LocalDateTime> occurrencesBetween(LocalDateTime anchor, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> occurrences = new ArrayList<>();
        LocalDateTime currentDate = anchor;
        // J'inverse les conditions des dates, pour avoir une plage inclusive
        while (!currentDate.isAfter(end) && (until == null || !currentDate.toLocalDate().isAfter(until))) {
            if (!currentDate.isBefore(start) && !exDates.contains(currentDate.toLocalDate())) {
                occurrences.add(currentDate);
            }
            if (interval != null && interval > 0) {
                currentDate = getNextDate(currentDate);
            } else {
                break;
            }

        }
        return occurrences;
    }
}
