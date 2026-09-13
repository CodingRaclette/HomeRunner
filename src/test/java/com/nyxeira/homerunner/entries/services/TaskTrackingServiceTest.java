package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.entries.events.ContributionAddedEvent;
import com.nyxeira.homerunner.entries.events.ContributionRemovedEvent;
import com.nyxeira.homerunner.entries.events.SelfAssignEvent;
import com.nyxeira.homerunner.entries.events.SelfUnassignEvent;
import com.nyxeira.homerunner.entries.events.TaskValidatedByOtherEvent;
import com.nyxeira.homerunner.entries.exceptions.UserNotParticipantException;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.occurrences.TaskOccurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * TaskTrackingService avec EntryRepository/UserRepository/ApplicationEventPublisher mockes,
 * dans le meme style que EntryLifeServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class TaskTrackingServiceTest {

    @Mock
    EntryRepository entryRepository;
    @Mock
    TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ApplicationEventPublisher publisher;

    private TaskTrackingService service() {
        return new TaskTrackingService(entryRepository, taskOccurrenceRepository, userRepository, publisher);
    }

    private Task aTask(User creator) {
        TaskDTO dto = new TaskDTO();
        dto.setName("Sortir les poubelles");
        return new Task(dto, creator);
    }

    @Test
    void selfAssignAjouteLActeurAuxParticipantsEtPublieSelfAssignEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));

        service().selfAssign(5L, "bob");

        assertThat(task.getParticipants()).containsExactly(actor);
        ArgumentCaptor<SelfAssignEvent> captor = ArgumentCaptor.forClass(SelfAssignEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().login()).isEqualTo("bob");
        assertThat(captor.getValue().date()).isNull();
    }

    @Test
    void selfUnassignRetireLActeurDesParticipantsEtPublieSelfUnassignEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        task.setParticipants(new HashSet<>(Set.of(actor)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));

        service().selfUnassign(5L, "bob");

        assertThat(task.getParticipants()).isEmpty();
        ArgumentCaptor<SelfUnassignEvent> captor = ArgumentCaptor.forClass(SelfUnassignEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().login()).isEqualTo("bob");
        assertThat(captor.getValue().date()).isNull();
    }

    @Test
    void toggleContributorAjouteLaContributionQuandLActeurSeMarqueLuiMeme() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        task.setParticipants(new HashSet<>(Set.of(actor)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(actor));

        service().toggleContributor(5L, 2L, "bob");

        assertThat(task.getContributors()).containsExactly(actor);
        ArgumentCaptor<ContributionAddedEvent> captor = ArgumentCaptor.forClass(ContributionAddedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().targetLogin()).isEqualTo("bob");
        assertThat(captor.getValue().ActorLogin()).isEqualTo("bob");
    }

    @Test
    void toggleContributorRetireLaContributionQuandLActeurSeDemarqueLuiMeme() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        task.setParticipants(new HashSet<>(Set.of(actor)));
        task.addContributor(actor);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(actor));

        service().toggleContributor(5L, 2L, "bob");

        assertThat(task.getContributors()).isEmpty();
        ArgumentCaptor<ContributionRemovedEvent> captor = ArgumentCaptor.forClass(ContributionRemovedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().targetLogin()).isEqualTo("bob");
    }

    @Test
    void toggleContributorAutoriseLeCreateurABasculerUnAutreParticipant() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User target = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        task.setParticipants(new HashSet<>(Set.of(target)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        service().toggleContributor(5L, 2L, "alice");

        assertThat(task.getContributors()).containsExactly(target);
        ArgumentCaptor<ContributionAddedEvent> captor = ArgumentCaptor.forClass(ContributionAddedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().targetLogin()).isEqualTo("bob");
        assertThat(captor.getValue().ActorLogin()).isEqualTo("alice");
    }

    @Test
    void toggleContributorRefuseUnParticipantNonCreateurPourUnAutre() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        User target = UserTestBuilder.aUser().withLogin("charlie").build();
        Task task = aTask(creator);
        task.setParticipants(new HashSet<>(Set.of(actor, target)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service().toggleContributor(5L, 3L, "bob"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(task.getContributors()).isEmpty();
        verifyNoInteractions(publisher);
    }

    @Test
    void toggleContributorRefuseSiLaCibleNEstPasParticipante() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User target = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        // target n'est pas ajoute aux participants de la tache
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service().toggleContributor(5L, 2L, "alice"))
                .isInstanceOf(UserNotParticipantException.class);

        verifyNoInteractions(publisher);
    }

    @Test
    void toggleValidatedByOtherActiveLeFlagPourLeCreateurEtPublieLEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Task task = aTask(creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        service().toggleValidatedByOther(5L, "alice");

        assertThat(task.isValidatedByOther()).isTrue();
        ArgumentCaptor<TaskValidatedByOtherEvent> captor = ArgumentCaptor.forClass(TaskValidatedByOtherEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().state()).isTrue();
        assertThat(captor.getValue().actorLogin()).isEqualTo("alice");
    }

    @Test
    void toggleValidatedByOtherDesactiveLeFlagAuDeuxiemeAppel() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Task task = aTask(creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        service().toggleValidatedByOther(5L, "alice");
        service().toggleValidatedByOther(5L, "alice");

        assertThat(task.isValidatedByOther()).isFalse();
        ArgumentCaptor<TaskValidatedByOtherEvent> captor = ArgumentCaptor.forClass(TaskValidatedByOtherEvent.class);
        verify(publisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues().get(0).state()).isTrue();
        assertThat(captor.getAllValues().get(1).state()).isFalse();
    }

    @Test
    void toggleValidatedByOtherRefuseSiLActeurNEstPasLeCreateur() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User autre = UserTestBuilder.aUser().withLogin("bob").build();
        Task task = aTask(creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> service().toggleValidatedByOther(5L, "bob"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(task.isValidatedByOther()).isFalse();
        verifyNoInteractions(publisher);
    }

    @Test
    void getTaskLeveIllegalArgumentExceptionSiLEntreeNEstPasUneTache() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Event event = new Event(new EventDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> service().selfAssign(5L, "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Suivi au niveau d'une occurrence precise (tache recurrente) ---
    // resolveTrackable(taskId, date) delegue a TaskOccurrenceRepository : materialise
    // l'occurrence si besoin, sans jamais toucher a la master.

    @Test
    void selfAssignAvecDateMaterialiseLOccurrenceEtLuiAjouteLActeurSansToucherALaMaster() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task master = aTask(creator);
        LocalDate date = LocalDate.of(2026, 9, 20);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(master));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(taskOccurrenceRepository.findByMasterIdAndDate(5L, date)).thenReturn(Optional.empty());
        when(taskOccurrenceRepository.save(any(TaskOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));

        service().selfAssign(5L, date, "bob");

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);
        verify(taskOccurrenceRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants()).containsExactly(actor);
        assertThat(master.getParticipants()).isEmpty();
        ArgumentCaptor<SelfAssignEvent> eventCaptor = ArgumentCaptor.forClass(SelfAssignEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().date()).isEqualTo(date);
    }

    @Test
    void selfAssignAvecDateReutiliseLOccurrenceDejaMaterialiseeSansEnRecreerUne() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task master = aTask(creator);
        LocalDate date = LocalDate.of(2026, 9, 20);
        TaskOccurrence existing = new TaskOccurrence(master, date);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(master));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(taskOccurrenceRepository.findByMasterIdAndDate(5L, date)).thenReturn(Optional.of(existing));

        service().selfAssign(5L, date, "bob");

        assertThat(existing.getParticipants()).containsExactly(actor);
        verify(taskOccurrenceRepository, never()).save(any());
    }

    @Test
    void selfUnassignAvecDateRetireLActeurDeLOccurrenceSansToucherALaMaster() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task master = aTask(creator);
        master.getParticipants().add(actor); // participant sur la serie entiere
        LocalDate date = LocalDate.of(2026, 9, 20);
        TaskOccurrence existing = new TaskOccurrence(master, date);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(master));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(taskOccurrenceRepository.findByMasterIdAndDate(5L, date)).thenReturn(Optional.of(existing));

        service().selfUnassign(5L, date, "bob");

        // l'occurrence est desormais surchargee (sans bob) mais la master n'est pas modifiee
        assertThat(existing.getParticipants()).doesNotContain(actor);
        assertThat(master.getParticipants()).containsExactly(actor);
        ArgumentCaptor<SelfUnassignEvent> eventCaptor = ArgumentCaptor.forClass(SelfUnassignEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().date()).isEqualTo(date);
    }

    @Test
    void toggleContributorAvecDateAgitSurLesContributeursDeLOccurrenceUniquement() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User actor = UserTestBuilder.aUser().withLogin("bob").build();
        Task master = aTask(creator);
        master.getParticipants().add(actor);
        LocalDate date = LocalDate.of(2026, 9, 20);
        TaskOccurrence existing = new TaskOccurrence(master, date);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(master));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(actor));
        when(taskOccurrenceRepository.findByMasterIdAndDate(5L, date)).thenReturn(Optional.of(existing));

        service().toggleContributor(5L, date, 2L, "bob");

        assertThat(existing.getContributors()).containsExactly(actor);
        assertThat(master.getContributors()).isEmpty();
        ArgumentCaptor<ContributionAddedEvent> eventCaptor = ArgumentCaptor.forClass(ContributionAddedEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().date()).isEqualTo(date);
    }

    @Test
    void toggleValidatedByOtherAvecDateNAffecteQueLOccurrence() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Task master = aTask(creator);
        LocalDate date = LocalDate.of(2026, 9, 20);
        TaskOccurrence existing = new TaskOccurrence(master, date);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(master));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));
        when(taskOccurrenceRepository.findByMasterIdAndDate(5L, date)).thenReturn(Optional.of(existing));

        service().toggleValidatedByOther(5L, date, "alice");

        assertThat(existing.isValidatedByOther()).isTrue();
        assertThat(master.isValidatedByOther()).isFalse();
        ArgumentCaptor<TaskValidatedByOtherEvent> eventCaptor = ArgumentCaptor.forClass(TaskValidatedByOtherEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().date()).isEqualTo(date);
    }
}
