package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.dto.CalendarItemDTO;
import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.model.RecurrenceRule;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CalendarService assemble en un seul flux : les entrees non recurrentes (une occurrence =
 * l'entree elle-meme), les entrees recurrentes expansees par RecurrenceRule.occurrencesBetween,
 * et pour chaque date calculee la substitution eventuelle par une occurrence materialisee
 * (participants/titre/description/endDate surcharges). Entry/Event/Task sont mockees plutot
 * que construites via leur DTO : seul CalendarService::getCalendar est sous test ici, la
 * mecanique d'expansion elle-meme est couverte par RecurrenceRuleTest, et le mapping par
 * CalendarItemDTOTest.
 */
@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    EntryRepository entryRepository;
    @Mock
    EventOccurrenceRepository eventOccurrenceRepository;
    @Mock
    TaskOccurrenceRepository taskOccurrenceRepository;

    private CalendarService service() {
        return new CalendarService(entryRepository, eventOccurrenceRepository, taskOccurrenceRepository);
    }

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 9, 30, 23, 59);

    @Test
    void uneEntreeNonRecurrenteProduitUnSeulItemASaPropreDate() {
        Task task = mockTask(1L, "Sortir les poubelles", LocalDateTime.of(2026, 9, 5, 8, 0), false);
        when(entryRepository.findInPeriodOrRecurring(START, END, START.toLocalDate())).thenReturn(List.of(task));
        when(eventOccurrenceRepository.findByMasterIdInAndDateBetween(anyList(), any(), any())).thenReturn(List.of());
        when(taskOccurrenceRepository.findByMasterIdInAndDateBetween(anyList(), any(), any())).thenReturn(List.of());

        List<CalendarItemDTO> calendar = service().getCalendar(START, END);

        assertThat(calendar).hasSize(1);
        assertThat(calendar.get(0).getTitle()).isEqualTo("Sortir les poubelles");
        assertThat(calendar.get(0).getStartDate()).isEqualTo(LocalDateTime.of(2026, 9, 5, 8, 0));
    }

    @Test
    void uneEntreeRecurrenteEstExpanseeSurTouteLaPeriodeSansOccurrenceMaterialisee() {
        Event event = mockEvent(1L, "Reunion hebdo",
                LocalDateTime.of(2026, 9, 1, 10, 0), LocalDateTime.of(2026, 9, 1, 11, 0),
                new RecurrenceRule(Frequency.WEEKLY, 1, null));
        when(entryRepository.findInPeriodOrRecurring(START, END, START.toLocalDate())).thenReturn(List.of(event));
        when(eventOccurrenceRepository.findByMasterIdInAndDateBetween(List.of(1L), START.toLocalDate(), END.toLocalDate()))
                .thenReturn(List.of());
        when(taskOccurrenceRepository.findByMasterIdInAndDateBetween(List.of(), START.toLocalDate(), END.toLocalDate()))
                .thenReturn(List.of());

        List<CalendarItemDTO> calendar = service().getCalendar(START, END);

        // WEEKLY a partir du 1er septembre sur tout le mois : 1, 8, 15, 22, 29
        assertThat(calendar).hasSize(5);
        assertThat(calendar).allSatisfy(item -> assertThat(item.getTitle()).isEqualTo("Reunion hebdo"));
        assertThat(calendar.stream().map(CalendarItemDTO::getStartDate).toList()).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 8, 10, 0),
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 22, 10, 0),
                LocalDateTime.of(2026, 9, 29, 10, 0)
        );
    }

    @Test
    void uneOccurrenceMaterialiseeRemplaceLItemCalculePourSaDateUniquement() {
        Event event = mockEvent(1L, "Reunion hebdo",
                LocalDateTime.of(2026, 9, 1, 10, 0), LocalDateTime.of(2026, 9, 1, 11, 0),
                new RecurrenceRule(Frequency.WEEKLY, 1, null));
        EventOccurrence materialized = new EventOccurrence(event, LocalDate.of(2026, 9, 8));
        EventDTO overrides = new EventDTO();
        overrides.setName("Reunion (exceptionnellement en visio)");
        materialized.applyOverrides(overrides);
        when(entryRepository.findInPeriodOrRecurring(START, END, START.toLocalDate())).thenReturn(List.of(event));
        when(eventOccurrenceRepository.findByMasterIdInAndDateBetween(List.of(1L), START.toLocalDate(), END.toLocalDate()))
                .thenReturn(List.of(materialized));
        when(taskOccurrenceRepository.findByMasterIdInAndDateBetween(List.of(), START.toLocalDate(), END.toLocalDate()))
                .thenReturn(List.of());

        List<CalendarItemDTO> calendar = service().getCalendar(START, END);

        assertThat(calendar).hasSize(5);
        assertThat(calendar.get(1).getStartDate()).isEqualTo(LocalDateTime.of(2026, 9, 8, 10, 0));
        assertThat(calendar.get(1).getTitle()).isEqualTo("Reunion (exceptionnellement en visio)");
        // les autres occurrences de la serie ne sont pas affectees
        assertThat(calendar.get(0).getTitle()).isEqualTo("Reunion hebdo");
        assertThat(calendar.get(2).getTitle()).isEqualTo("Reunion hebdo");
    }

    @Test
    void neRechercheDesOccurrencesMaterialiseesQueParmiLesEntreesRecurrentesDuBonType() {
        Task nonRecurringTask = mockTask(1L, "Tache ponctuelle", LocalDateTime.of(2026, 9, 5, 8, 0), false);
        Event recurringEvent = mockEvent(2L, "Reunion hebdo",
                LocalDateTime.of(2026, 9, 1, 10, 0), LocalDateTime.of(2026, 9, 1, 11, 0),
                new RecurrenceRule(Frequency.WEEKLY, 1, null));
        Task recurringTask = mockTask(3L, "Tache recurrente", LocalDateTime.of(2026, 9, 2, 8, 0), true);
        when(recurringTask.getRecurrence()).thenReturn(new RecurrenceRule(Frequency.DAILY, 7, null));
        when(entryRepository.findInPeriodOrRecurring(START, END, START.toLocalDate()))
                .thenReturn(List.of(nonRecurringTask, recurringEvent, recurringTask));
        when(eventOccurrenceRepository.findByMasterIdInAndDateBetween(anyList(), any(), any())).thenReturn(List.of());
        when(taskOccurrenceRepository.findByMasterIdInAndDateBetween(anyList(), any(), any())).thenReturn(List.of());

        service().getCalendar(START, END);

        ArgumentCaptor<List<Long>> eventIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventOccurrenceRepository).findByMasterIdInAndDateBetween(eventIdsCaptor.capture(), any(), any());
        assertThat(eventIdsCaptor.getValue()).containsExactly(2L);

        ArgumentCaptor<List<Long>> taskIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskOccurrenceRepository).findByMasterIdInAndDateBetween(taskIdsCaptor.capture(), any(), any());
        assertThat(taskIdsCaptor.getValue()).containsExactly(3L);
    }

    private Event mockEvent(Long id, String name, LocalDateTime date, LocalDateTime endDate, RecurrenceRule recurrence) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(id);
        when(event.getType()).thenReturn(EntryType.EVENT);
        when(event.getName()).thenReturn(name);
        when(event.getDate()).thenReturn(date);
        when(event.getEndDate()).thenReturn(endDate);
        when(event.isRecurring()).thenReturn(recurrence != null);
        if (recurrence != null) {
            when(event.getRecurrence()).thenReturn(recurrence);
        }
        return event;
    }

    private Task mockTask(Long id, String name, LocalDateTime date, boolean recurring) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getType()).thenReturn(EntryType.TASK);
        when(task.getName()).thenReturn(name);
        when(task.getDate()).thenReturn(date);
        when(task.isRecurring()).thenReturn(recurring);
        return task;
    }
}
