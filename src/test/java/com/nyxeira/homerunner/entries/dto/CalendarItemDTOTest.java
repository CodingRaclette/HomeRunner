package com.nyxeira.homerunner.entries.dto;

import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.entries.model.occurrences.TaskOccurrence;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapping entree/occurrence -> item de calendrier affiche (CalendarService). Le point le
 * plus fragile est le report de la duree de la master sur la date de chaque occurrence
 * calculee (endDate n'existe qu'au niveau de l'entree/occurrence "ancre").
 */
class CalendarItemDTOTest {

    @Test
    void fromEntryAtDateReporteLaDureeDeLEventSurLaDateDOccurrence() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 30));
        Event event = new Event(dto, UserTestBuilder.aUser().build());
        LocalDateTime occurrenceDate = LocalDateTime.of(2026, 9, 8, 10, 0);

        CalendarItemDTO item = CalendarItemDTO.fromEntryAtDate(event, occurrenceDate, true);

        assertThat(item.getTitle()).isEqualTo("Reunion");
        assertThat(item.getStartDate()).isEqualTo(occurrenceDate);
        assertThat(item.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 8, 11, 30));
    }

    @Test
    void fromEntryAtDateReporteLEtatDoneDUneTache() {
        Task task = new Task(new TaskDTO(), UserTestBuilder.aUser().build());
        task.setValidatedByOther(true);
        LocalDateTime occurrenceDate = LocalDateTime.of(2026, 9, 8, 8, 0);

        CalendarItemDTO item = CalendarItemDTO.fromEntryAtDate(task, occurrenceDate, false);

        assertThat(item.isDone()).isTrue();
        assertThat(item.getEndDate()).isNull();
    }

    @Test
    void fromOccurrenceAtDateUtiliseLaDureeDeLaMasterQuandEndDateNEstPasSurchargee() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        Event master = new Event(dto, UserTestBuilder.aUser().build());
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));
        LocalDateTime occurrenceDateTime = LocalDateTime.of(2026, 9, 8, 10, 0);

        CalendarItemDTO item = CalendarItemDTO.fromOccurrenceAtDate(occurrence, occurrenceDateTime);

        assertThat(item.getTitle()).isEqualTo("Reunion");
        assertThat(item.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 8, 11, 0));
    }

    @Test
    void fromOccurrenceAtDateUtiliseLEndDateSurchargeeQuandElleExiste() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        Event master = new Event(dto, UserTestBuilder.aUser().build());
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));
        occurrence.overrideEndDate(LocalDateTime.of(2026, 9, 8, 15, 0));
        LocalDateTime occurrenceDateTime = LocalDateTime.of(2026, 9, 8, 10, 0);

        CalendarItemDTO item = CalendarItemDTO.fromOccurrenceAtDate(occurrence, occurrenceDateTime);

        assertThat(item.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 8, 15, 0));
    }

    @Test
    void fromOccurrenceAtDateReporteLEtatDoneDUneTaskOccurrence() {
        Task master = new Task(new TaskDTO(), UserTestBuilder.aUser().build());
        TaskOccurrence occurrence = new TaskOccurrence(master, LocalDate.of(2026, 9, 8));
        occurrence.setValidatedByOther(true);
        LocalDateTime occurrenceDateTime = LocalDateTime.of(2026, 9, 8, 8, 0);

        CalendarItemDTO item = CalendarItemDTO.fromOccurrenceAtDate(occurrence, occurrenceDateTime);

        assertThat(item.isDone()).isTrue();
        // la master n'est elle-meme pas modifiee
        assertThat(master.isValidatedByOther()).isFalse();
    }

    @Test
    void fromOccurrenceAtDateReporteLeTitreSurchargeDeLOccurrence() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        Event master = new Event(dto, UserTestBuilder.aUser().build());
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));
        EventDTO overrides = new EventDTO();
        overrides.setName("Reunion (exceptionnellement en visio)");
        occurrence.applyOverrides(overrides);

        CalendarItemDTO item = CalendarItemDTO.fromOccurrenceAtDate(occurrence, LocalDateTime.of(2026, 9, 8, 10, 0));

        assertThat(item.getTitle()).isEqualTo("Reunion (exceptionnellement en visio)");
    }
}
