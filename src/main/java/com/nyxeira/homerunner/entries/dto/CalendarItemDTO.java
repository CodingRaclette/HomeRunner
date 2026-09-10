package com.nyxeira.homerunner.entries.dto;

import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;

public class CalendarItemDTO {

    Long entryId;
    LocalDateTime occurrenceDate;
    String type;
    String title;
    LocalDateTime startDate;
    LocalDateTime endDate;
    boolean done;


    public static CalendarItemDTO fromEntryAtDate(Entry entry, LocalDateTime occurrenceDate) {
        CalendarItemDTO dto = new CalendarItemDTO();
        dto.entryId = entry.getId();
        dto.occurrenceDate = occurrenceDate;
        dto.type = entry.getType().name();
        dto.title = entry.getName();
        dto.startDate = occurrenceDate;

        if (entry instanceof Event event) {
            Duration duration = Duration.between(event.getDate(), event.getEndDate());
            dto.endDate = occurrenceDate.plus(duration);
        }
        if (entry instanceof Task task) {
            dto.done = task.isDone();
        }
        return dto;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public LocalDateTime getOccurrenceDate() {
        return occurrenceDate;
    }

    public void setOccurrenceDate(LocalDateTime occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
