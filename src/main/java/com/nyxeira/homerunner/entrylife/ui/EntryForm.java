package com.nyxeira.homerunner.entrylife.ui;

import com.nyxeira.homerunner.entrylife.dto.EventDTO;
import com.nyxeira.homerunner.entrylife.dto.TaskDTO;
import com.nyxeira.homerunner.entrymodel.model.EntryType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class EntryForm {
    @NotNull
    private EntryType type;
    @NotBlank
    private String name;
    @NotNull
    private LocalDateTime date;
    private LocalDateTime endDate; // pertinent seulement si type == EVENT
    private String description;
    private List<Long> participantIds;
    private List<Long> assigneeIds;

    // cohérence des dates : @AssertTrue déclaratif plutôt qu'un if dans le contrôleur
    @AssertTrue(message = "La date de fin doit être postérieure à la date de début.")
    public boolean isEndDateValid() {
        return type != EntryType.EVENT || endDate == null || endDate.isAfter(date);
    }

    public EventDTO toEventData() {
        EventDTO d = new EventDTO();
        d.setName(name);
        d.setDate(date);
        d.setDescription(description);
        d.setEndDate(endDate);
        d.setParticipantIds(participantIds);
        return d;
    }

    public TaskDTO toTaskData() {
        TaskDTO d = new TaskDTO();
        d.setName(name);
        d.setDate(date);
        d.setDescription(description);
        d.setAssigneeIds(assigneeIds);
        return d;
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

    public List<Long> getAssigneeIds() {
        return assigneeIds;
    }

    public void setAssigneeIds(List<Long> assigneeIds) {
        this.assigneeIds = assigneeIds;
    }
}
