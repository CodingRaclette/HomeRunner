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
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
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
    UserRepository userRepository;
    @Mock
    ApplicationEventPublisher publisher;

    private TaskTrackingService service() {
        return new TaskTrackingService(entryRepository, userRepository, publisher);
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

        assertThatThrownBy(() -> service().selfAssign(5L, "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
