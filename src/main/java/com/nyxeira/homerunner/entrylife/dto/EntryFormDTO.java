package com.nyxeira.homerunner.entrylife.dto;

import com.nyxeira.homerunner.entrymodel.model.Entry;
import com.nyxeira.homerunner.entrymodel.model.EntryType;
import com.nyxeira.homerunner.entrymodel.model.Event;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public class EntryFormDTO {
    private Long id;
    @NotNull
    private EntryType type;
    @NotBlank
    private String name;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime date;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate; // EVENT only

    private String description;

    private List<Long> participantIds;


    // cohérence des dates : @AssertTrue déclaratif plutôt qu'un if dans le contrôleur
    @AssertTrue(message = "La date de fin doit être postérieure à la date de début.")
    public boolean isEndDateValid() {
        return type != EntryType.EVENT || endDate == null || endDate.isAfter(date);
    }

    public EventDTO toEventDTO() {
        EventDTO d = new EventDTO();
        d.setName(getName());
        d.setDate(getDate());
        d.setDescription(getDescription());
        d.setEndDate(getEndDate());
        d.setParticipantIds(getParticipantIds());
        return d;
    }

    public TaskDTO toTaskDTO() {
        TaskDTO d = new TaskDTO();
        d.setName(getName());
        d.setDate(getDate());
        d.setDescription(getDescription());
        d.setParticipantIds(getParticipantIds());
        return d;
    }

    public EntryFormDTO fromEntry(Entry e) {
        EntryFormDTO form = new EntryFormDTO();
        form.setId(e.getId());
        form.setType(e.getType());
        form.setName(e.getName());
        form.setDate(e.getDate());
        form.setDescription(e.getDescription());
        form.setParticipantIds(e.getParticipantIds());
        if (e.getType().equals(EntryType.EVENT)) {
            Event event = (Event)e;
            form.setEndDate(event.getEndDate());
        }
        return form;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

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

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
