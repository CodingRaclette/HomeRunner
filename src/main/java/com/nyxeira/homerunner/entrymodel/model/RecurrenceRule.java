package com.nyxeira.homerunner.entrymodel.model;


import jakarta.persistence.*;

import java.time.LocalDate;
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
    Set<LocalDate> exDates;

    public RecurrenceRule() {}

    public Frequency getFrequency() { return frequency; }
    public Integer getInterval() { return interval; }
    public LocalDate getUntil() { return until; }
    public Set<LocalDate> getExDates() { return exDates; }
}
