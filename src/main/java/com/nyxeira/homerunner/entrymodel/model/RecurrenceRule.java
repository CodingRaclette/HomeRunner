package com.nyxeira.homerunner.entrymodel.model;


import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.util.Set;


@Embeddable
public class RecurrenceRule {

    @Enumerated(EnumType.STRING)
    Frequency frequency; // Basée sur un enum non développé explicitement lors de la conception

    @Column(name = "recurrence_interval") // "interval" est un mot réservé SQL (H2)
    int interval;

    LocalDate until;

    @ElementCollection
    Set<LocalDate> exDates;

    public RecurrenceRule() {}

    public Frequency getFrequency() { return frequency; }
    public int getInterval() { return interval; }
    public LocalDate getUntil() { return until; }
    public Set<LocalDate> getExDates() { return exDates; }
}
