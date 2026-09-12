package com.nyxeira.homerunner.notification.services;

import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.notification.model.Notification;
import com.nyxeira.homerunner.notification.model.ScheduledReminder;
import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.notification.repositories.ScheduledReminderRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReminderScheduler avec ses repositories mockés, dans le même style que les autres tests de
 * service : on fige "maintenant" implicitement via des entrées dont les dates sont ancrées dans
 * le futur proche, pour ne pas dépendre de LocalDate.now() dans les assertions.
 */
@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock EntryRepository entryRepository;
    @Mock EventOccurrenceRepository eventOccurrenceRepository;
    @Mock TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock ScheduledReminderRepository scheduledReminderRepository;
    @Mock NotificationRepository notificationRepository;

    private ReminderScheduler scheduler() {
        return new ReminderScheduler(entryRepository, eventOccurrenceRepository, taskOccurrenceRepository,
                scheduledReminderRepository, notificationRepository);
    }

    @Test
    void materializeUpcomingRemindersCreeLeRappelDUneOccurrenceNiMaterialiseeNiDejaProgrammee() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion hebdo");
        dto.setDate(LocalDateTime.now().plusHours(1));
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        dto.setReminderMinutesBefore(10);
        // Borne la récurrence à une seule occurrence dans la fenêtre glissante (la prochaine
        // tombant dans 7 jours), pour que le test reste déterministe.
        dto.setUntil(LocalDate.now().plusDays(3));
        Event event = new Event(dto, creator);
        when(entryRepository.findRecurringWithReminder(any())).thenReturn(List.of(event));
        when(eventOccurrenceRepository.findByMasterIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(scheduledReminderRepository.existsByEntryAndOccurrenceDate(any(), any())).thenReturn(false);

        scheduler().materializeUpcomingReminders();

        ArgumentCaptor<ScheduledReminder> captor = ArgumentCaptor.forClass(ScheduledReminder.class);
        verify(scheduledReminderRepository).save(captor.capture());
        assertThat(captor.getValue().getEntry()).isEqualTo(event);
        assertThat(captor.getValue().getOccurrenceDate()).isEqualTo(dto.getDate().toLocalDate());
        assertThat(captor.getValue().getFireAt()).isEqualTo(dto.getDate().minusMinutes(10));
    }

    @Test
    void materializeUpcomingRemindersIgnoreLesOccurrencesDejaMaterialisees() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion hebdo");
        dto.setDate(LocalDateTime.now().plusHours(1));
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        dto.setReminderMinutesBefore(10);
        Event event = new Event(dto, creator);
        when(entryRepository.findRecurringWithReminder(any())).thenReturn(List.of(event));
        when(eventOccurrenceRepository.findByMasterIdAndDate(any(), any()))
                .thenReturn(Optional.of(new EventOccurrence(event, dto.getDate().toLocalDate())));

        scheduler().materializeUpcomingReminders();

        verify(scheduledReminderRepository, never()).save(any());
    }

    @Test
    void materializeUpcomingRemindersNeRecreePasUnRappelDejaProgramme() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion hebdo");
        dto.setDate(LocalDateTime.now().plusHours(1));
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        dto.setReminderMinutesBefore(10);
        Event event = new Event(dto, creator);
        when(entryRepository.findRecurringWithReminder(any())).thenReturn(List.of(event));
        when(eventOccurrenceRepository.findByMasterIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(scheduledReminderRepository.existsByEntryAndOccurrenceDate(any(), any())).thenReturn(true);

        scheduler().materializeUpcomingReminders();

        verify(scheduledReminderRepository, never()).save(any());
    }

    @Test
    void pollAndSendNotifieChaqueParticipantDUnRappelDuEtLeMarqueEnvoye() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User participant = UserTestBuilder.aUser().withLogin("bob").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        Event event = new Event(dto, creator);
        event.setParticipants(new HashSet<>(Set.of(creator, participant)));
        ScheduledReminder reminder = new ScheduledReminder(event, LocalDateTime.now().minusMinutes(1), null);
        when(scheduledReminderRepository.findBySentFalseAndFireAtLessThanEqual(any())).thenReturn(List.of(reminder));

        scheduler().pollAndSend();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getRecipient)
                .containsExactlyInAnyOrder(creator, participant);
        assertThat(captor.getValue().getMessage()).contains("Rappel").contains("Reunion");
        assertThat(reminder.isSent()).isTrue();
    }

    @Test
    void pollAndSendMentionneLaDateDOccurrenceDansLeMessageEtLeLienQuandElleEstPresente() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion hebdo");
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        Event event = new Event(dto, creator);
        event.setParticipants(new HashSet<>(Set.of(creator)));
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 8);
        ScheduledReminder reminder = new ScheduledReminder(event, LocalDateTime.now().minusMinutes(1), occurrenceDate);
        when(scheduledReminderRepository.findBySentFalseAndFireAtLessThanEqual(any())).thenReturn(List.of(reminder));

        scheduler().pollAndSend();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("2026-09-08");
        assertThat(captor.getValue().getLink()).contains("date=2026-09-08");
    }
}
