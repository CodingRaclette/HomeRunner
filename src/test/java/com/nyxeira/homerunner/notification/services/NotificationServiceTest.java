package com.nyxeira.homerunner.notification.services;

import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.entries.events.*;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.notification.model.Notification;
import com.nyxeira.homerunner.notification.model.ScheduledReminder;
import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.notification.repositories.ScheduledReminderRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

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
 * NotificationService avec ses repositories mockés : on vérifie la logique "qui notifier /
 * quand programmer un rappel", indépendamment de la base, dans le même style que
 * EntryLifeServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock EntryRepository entryRepository;
    @Mock EventOccurrenceRepository eventOccurrenceRepository;
    @Mock TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock ScheduledReminderRepository scheduledReminderRepository;

    private NotificationService service() {
        return new NotificationService(entryRepository, eventOccurrenceRepository, taskOccurrenceRepository,
                userRepository, notificationRepository, scheduledReminderRepository);
    }

    @Test
    void onEntryCreatedNotifieLesParticipantsSaufLActeurEtNeCreeAucunRappelSansConfiguration() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User participant = UserTestBuilder.aUser().withLogin("bob").build();
        EventDTO dto = new EventDTO();
        dto.setName("Anniversaire");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        Event event = new Event(dto, creator);
        event.setParticipants(new HashSet<>(Set.of(creator, participant)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));

        service().onEntryCreated(new EntryCreatedEvent(5L, "alice"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(participant);
        verify(scheduledReminderRepository, never()).save(any());
    }

    @Test
    void onEntryCreatedProgrammeUnRappelUniquePourUneEntreeNonRecurrenteAvecRappelConfigure() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        dto.setReminderMinutesBefore(30);
        Event event = new Event(dto, creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));

        service().onEntryCreated(new EntryCreatedEvent(5L, "alice"));

        ArgumentCaptor<ScheduledReminder> captor = ArgumentCaptor.forClass(ScheduledReminder.class);
        verify(scheduledReminderRepository).save(captor.capture());
        assertThat(captor.getValue().getFireAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 18, 30));
        assertThat(captor.getValue().getOccurrenceDate()).isNull();
    }

    @Test
    void onEntryCreatedNeProgrammeAucunRappelPourUneEntreeRecurrenteMemeAvecRappelConfigure() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion hebdo");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        dto.setReminderMinutesBefore(30);
        Event event = new Event(dto, creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));

        service().onEntryCreated(new EntryCreatedEvent(5L, "alice"));

        // Les occurrences d'une entrée récurrente sont dépliées par ReminderScheduler (cf. 5.8),
        // pas ici.
        verify(scheduledReminderRepository, never()).save(any());
    }

    @Test
    void onEntryUpdatedNotifieAjoutsRetraitsEtParticipantsConservesDifferemment() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User kept = UserTestBuilder.aUser().withLogin("kept").build();
        User added = UserTestBuilder.aUser().withLogin("added").build();
        User removed = UserTestBuilder.aUser().withLogin("removed").build();
        EventDTO dto = new EventDTO();
        dto.setName("Repas");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        Event event = new Event(dto, creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(added));
        when(userRepository.findAllById(List.of(3L))).thenReturn(List.of(removed));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(kept));

        service().onEntryUpdated(new EntryUpdatedEvent(5L, List.of(1L, 3L), List.of(1L, 2L), "alice"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertThat(saved).extracting(Notification::getRecipient).containsExactlyInAnyOrder(added, removed, kept);
        assertThat(saved.stream().filter(n -> n.getRecipient() == added).findFirst().orElseThrow().getMessage())
                .contains("ajouté");
        assertThat(saved.stream().filter(n -> n.getRecipient() == removed).findFirst().orElseThrow().getMessage())
                .contains("retiré");
        assertThat(saved.stream().filter(n -> n.getRecipient() == kept).findFirst().orElseThrow().getMessage())
                .contains("modifiée");
    }

    @Test
    void onEntryUpdatedPurgeLesRappelsObsoletesEtEnRecalculeUnNouveauSiNonRecurrente() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        dto.setReminderMinutesBefore(15);
        Event event = new Event(dto, creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));

        service().onEntryUpdated(new EntryUpdatedEvent(5L, List.of(), List.of(), "alice"));

        verify(scheduledReminderRepository).deleteByEntryAndOccurrenceDate(event, null);
        verify(scheduledReminderRepository).deleteByEntryAndOccurrenceDateIsNotNullAndSentFalse(event);
        ArgumentCaptor<ScheduledReminder> captor = ArgumentCaptor.forClass(ScheduledReminder.class);
        verify(scheduledReminderRepository).save(captor.capture());
        assertThat(captor.getValue().getFireAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 18, 45));
    }

    @Test
    void onEntryDeletedNotifieLesParticipantsSansRechargerLEntreeEtSupprimeSesRappels() {
        User participant = UserTestBuilder.aUser().withLogin("bob").build();
        when(userRepository.findAllById(List.of(42L))).thenReturn(List.of(participant));
        Event entryRef = new Event();
        when(entryRepository.getReferenceById(5L)).thenReturn(entryRef);

        service().onEntryDeleted(new EntryDeletedEvent(5L, "Anniversaire", List.of(42L), "alice"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(participant);
        assertThat(captor.getValue().getMessage()).contains("Anniversaire").contains("supprimée");
        verify(entryRepository, never()).findById(any());
        verify(scheduledReminderRepository).deleteByEntry(entryRef);
    }

    @Test
    void onOccurrenceCancelledNotifieLesParticipantsEtSupprimeLeRappelDeCetteDateUniquement() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User participant = UserTestBuilder.aUser().withLogin("bob").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        Event event = new Event(dto, creator);
        event.setParticipants(new HashSet<>(Set.of(creator, participant)));
        LocalDate date = LocalDate.of(2026, 9, 20);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));

        service().onOccurrenceCancelled(new OccurrenceCancelledEvent(5L, date, "alice"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(participant);
        verify(scheduledReminderRepository).deleteByEntryAndOccurrenceDate(event, date);
    }

    @Test
    void onOccurrenceUpdatedCreeLeRappelDeLOccurrenceSAucunNExisteEncore() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        dto.setReminderMinutesBefore(10);
        Event event = new Event(dto, creator);
        LocalDate date = LocalDate.of(2026, 9, 8);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(eventOccurrenceRepository.findByMasterIdAndDate(event.getId(), date)).thenReturn(Optional.empty());
        when(scheduledReminderRepository.existsByEntryAndOccurrenceDate(event, date)).thenReturn(false);

        service().onOccurrenceUpdated(new OccurrenceUpdatedEvent(5L, date, "alice"));

        ArgumentCaptor<ScheduledReminder> captor = ArgumentCaptor.forClass(ScheduledReminder.class);
        verify(scheduledReminderRepository).save(captor.capture());
        assertThat(captor.getValue().getOccurrenceDate()).isEqualTo(date);
        assertThat(captor.getValue().getFireAt()).isEqualTo(LocalDateTime.of(2026, 9, 8, 9, 50));
    }

    @Test
    void onOccurrenceUpdatedNeRecreeAucunRappelSUnExisteDeja() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        dto.setReminderMinutesBefore(10);
        Event event = new Event(dto, creator);
        LocalDate date = LocalDate.of(2026, 9, 8);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(eventOccurrenceRepository.findByMasterIdAndDate(event.getId(), date)).thenReturn(Optional.empty());
        when(scheduledReminderRepository.existsByEntryAndOccurrenceDate(event, date)).thenReturn(true);

        service().onOccurrenceUpdated(new OccurrenceUpdatedEvent(5L, date, "alice"));

        verify(scheduledReminderRepository, never()).save(any());
    }

    @Test
    void onSelfAssignedNotifieLeCreateurSaufSIlEstLActeur() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User assignee = UserTestBuilder.aUser().withLogin("bob").withName("Bob").build();
        Task task = new Task(new TaskDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(assignee));

        service().onSelfAssigned(new SelfAssignEvent(5L, "bob"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(creator);
        assertThat(captor.getValue().getMessage()).contains("Bob");
    }

    @Test
    void onSelfAssignedNeNotifiePersonneQuandLeCreateurSAssigneLuiMeme() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Task task = new Task(new TaskDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        service().onSelfAssigned(new SelfAssignEvent(5L, "alice"));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void onContributionAddedNotifieLaCibleSaufSiElleEstLActeur() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User target = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = new Task(new TaskDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(target));

        service().onContributionAdded(new ContributionAddedEvent(5L, "bob", "alice"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(target);
    }

    @Test
    void onTaskValidatedByOtherNotifieLesParticipantsSaufLActeur() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User participant = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = new Task(new TaskDTO(), creator);
        task.setParticipants(new HashSet<>(Set.of(creator, participant)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));

        service().onTaskValidatedByOther(new TaskValidatedByOtherEvent(5L, true, "alice"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo(participant);
        assertThat(captor.getValue().getMessage()).contains("validée manuellement");
    }
}
