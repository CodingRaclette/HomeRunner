package com.nyxeira.homerunner.entries.model.occurrences;

import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Event;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"master_id", "date"}))
@AssociationOverride(name = "participants", joinTable = @JoinTable(name = "event_occurrence_participants"))
public class EventOccurrence extends AbstractOccurrence {

    @ManyToOne
    Event master;

    LocalDateTime endDateOverride;

    public EventOccurrence() {}

    public EventOccurrence(Event master, LocalDate occurrenceDate) {
        this.master = master;
        this.date = occurrenceDate;
    }

    @Override
    public void applyOverrides(EntryDTO d) {
        super.applyOverrides(d);
        if (d instanceof EventDTO eventDto) {
            overrideEndDate(eventDto.getEndDate());
        }
    }

    @Override
    public Entry getMaster() {
        return this.master;
    }

    public LocalDateTime getEndDate() {
        return endDateOverride != null ? endDateOverride : master.getEndDate();
    }

    public boolean hasEndDateOverridden() { return endDateOverride != null; }
    public void overrideEndDate(LocalDateTime endDateOverride) {
        this.endDateOverride = endDateOverride;
    }
}
