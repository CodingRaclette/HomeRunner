package com.nyxeira.homerunner.entries.dto;

import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
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

    private Frequency frequency;
    // par defaut a 1 (et non 0) : le champ HTML a un attribut min="1", or quand la case
    // "Recurrent" n'est pas cochee ce champ reste dans le DOM (juste cache en display:none)
    // et un input number invalide (valeur 0 < min) bloque silencieusement la soumission
    // du formulaire cote navigateur (cf. recurrence-toggle.js qui desactive aussi le champ)
    private int interval = 1;
    private LocalDate until;

    private List<Long> participantIds;

    private Integer reminderMinutesBefore;


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
        d.setFrequency(getFrequency());
        d.setInterval(getInterval());
        d.setUntil(getUntil());
        d.setReminderMinutesBefore(getReminderMinutesBefore());
        return d;
    }

    public TaskDTO toTaskDTO() {
        TaskDTO d = new TaskDTO();
        d.setName(getName());
        d.setDate(getDate());
        d.setDescription(getDescription());
        d.setParticipantIds(getParticipantIds());
        d.setFrequency(getFrequency());
        d.setInterval(getInterval());
        d.setUntil(getUntil());
        d.setReminderMinutesBefore(getReminderMinutesBefore());
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
        form.setReminderMinutesBefore(e.getReminderMinutesBefore());
        if (e.getType().equals(EntryType.EVENT)) {
            Event event = (Event)e;
            form.setEndDate(event.getEndDate());
        }
        if (e.getRecurrence() != null) {
            form.setFrequency(e.getRecurrence().getFrequency());
            Integer interval = e.getRecurrence().getInterval();
            form.setInterval(interval == null || interval < 1 ? 1 : interval);
            form.setUntil(e.getRecurrence().getUntil());
        }
        return form;
    }

    // APour pré-remplir le formulaire d'édition d'une occurrence isolée
    // les champs recurrence (frequency/interval/until) n'ont pas de sens ici et restent donc vides.
    public EntryFormDTO fromEventOccurrence(EventOccurrence occurrence, LocalDateTime occurrenceDateTime) {
        EntryFormDTO form = new EntryFormDTO();
        form.setId(occurrence.getMaster().getId());
        form.setType(EntryType.EVENT);
        form.setName(occurrence.getName());
        form.setDate(occurrenceDateTime);
        form.setDescription(occurrence.getDescription());
        form.setEndDate(occurrence.getEndDate());
        form.setParticipantIds(occurrence.getParticipants().stream().map(User::getId).toList());
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

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setUntil(LocalDate until) {
        this.until = until;
    }

    public int getInterval() {
        return interval;
    }

    public LocalDate getUntil() {
        return until;
    }

    public Integer getReminderMinutesBefore() {
        return reminderMinutesBefore;
    }

    public void setReminderMinutesBefore(Integer reminderMinutesBefore) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }
}
