package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.dto.EntryFormDTO;
import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.entries.events.EntryCreatedEvent;
import com.nyxeira.homerunner.entries.events.EntryDeletedEvent;
import com.nyxeira.homerunner.entries.events.EntryUpdatedEvent;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.model.RecurrenceRule;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.entries.model.occurrences.Occurrence;
import com.nyxeira.homerunner.entries.model.occurrences.TaskOccurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EntryLifeService avec EntryRepository/UserRepository/ApplicationEventPublisher mockes :
 * on teste la logique metier (droits, evenements publies) independamment de la base et du
 * conteneur Spring, dans le style de UserManagementServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class EntryLifeServiceTest {

    @Mock
    EntryRepository entryRepository;
    @Mock
    EventOccurrenceRepository eventOccurrenceRepository;
    @Mock
    TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ApplicationEventPublisher publisher;

    private EntryLifeService service() {
        return new EntryLifeService(entryRepository, eventOccurrenceRepository, taskOccurrenceRepository, userRepository, publisher);
    }

    @Test
    void createEventEnregistreLEvenementAvecSesParticipantsEtPublieEntryCreatedEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User participant = UserTestBuilder.aUser().withLogin("bob").build();
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findAllById(List.of(42L))).thenReturn(List.of(participant));

        EventDTO dto = new EventDTO();
        dto.setName("Anniversaire");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 23, 0));
        dto.setParticipantIds(List.of(42L));

        service().createEvent(dto, "alice");

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(entryRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Anniversaire");
        assertThat(saved.getCreator()).isEqualTo(creator);
        assertThat(saved.getParticipants()).containsExactly(participant);

        ArgumentCaptor<EntryCreatedEvent> eventCaptor = ArgumentCaptor.forClass(EntryCreatedEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().actorLogin()).isEqualTo("alice");
    }

    @Test
    void createTaskEnregistreLaTacheSansParticipantsSiAucunNEstFourni() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        TaskDTO dto = new TaskDTO();
        dto.setName("Faire les courses");
        dto.setDate(LocalDateTime.of(2026, 9, 2, 10, 0));

        service().createTask(dto, "alice");

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(entryRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants()).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void getEntryForEditRenvoieLeFormulaireQuandLeCreateurDemande() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Anniversaire");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        Event event = new Event(dto, creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        EntryFormDTO form = service().getEntryForEdit(5L, "alice");

        assertThat(form.getName()).isEqualTo("Anniversaire");
        assertThat(form.getType()).isEqualTo(EntryType.EVENT);
    }

    @Test
    void getEntryForEditRefuseSiLUtilisateurNEstPasLeCreateur() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User autre = UserTestBuilder.aUser().withLogin("bob").build();
        Event event = new Event(new EventDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> service().getEntryForEdit(5L, "bob"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateEntryAppliqueLesDonneesEtPublieEntryUpdatedEventQuandLeCreateurModifie() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User oldParticipant = mock(User.class);
        when(oldParticipant.getId()).thenReturn(1L);
        User newParticipant = mock(User.class);
        when(newParticipant.getId()).thenReturn(2L);

        EventDTO initial = new EventDTO();
        initial.setName("Repas");
        initial.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        Event event = new Event(initial, creator);
        event.setParticipants(new HashSet<>(Set.of(oldParticipant)));
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(newParticipant));

        EventDTO update = new EventDTO();
        update.setName("Repas de famille");
        update.setDate(LocalDateTime.of(2026, 9, 2, 20, 0));
        update.setParticipantIds(List.of(2L));

        service().updateEntry(5L, update, "alice");

        assertThat(event.getName()).isEqualTo("Repas de famille");
        assertThat(event.getParticipants()).containsExactly(newParticipant);

        ArgumentCaptor<EntryUpdatedEvent> captor = ArgumentCaptor.forClass(EntryUpdatedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().oldParticipantIds()).containsExactly(1L);
        assertThat(captor.getValue().newParticipantIds()).containsExactly(2L);
    }

    @Test
    void updateEntryThrowSiUserNonAutorise() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User autre = UserTestBuilder.aUser().withLogin("bob").build();
        EventDTO initial = new EventDTO();
        initial.setName("Repas");
        Event event = new Event(initial, creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(autre));

        EventDTO update = new EventDTO();
        update.setName("Autre nom");

        assertThatThrownBy(() -> service().updateEntry(5L, update, "bob"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(event.getName()).isEqualTo("Repas");
        verifyNoInteractions(publisher);
    }

    @Test
    void deleteEntryMarqueLEntreeSupprimeeSansSuppressionPhysiqueEtPublieEntryDeletedEventQuandLeCreateurSupprime() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Event event = new Event(new EventDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        service().deleteEntry(5L, "alice");

        // Suppression logique : deletedAt est pose, mais l'entite n'est jamais physiquement
        // supprimee (le filtre @SQLRestriction sur Entry se charge de l'exclure des requetes).
        assertThat(event.getDeletedAt()).isNotNull();
        verify(entryRepository, never()).delete(any());
        ArgumentCaptor<EntryDeletedEvent> captor = ArgumentCaptor.forClass(EntryDeletedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().actorLogin()).isEqualTo("alice");
    }

    @Test
    void deleteEntryEstAutoriseePourUnAdminMemeSIlNEstPasLeCreateur() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User admin = UserTestBuilder.aUser().withLogin("admin").withRole(UserRole.ADMIN).build();
        Event event = new Event(new EventDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(admin));

        service().deleteEntry(5L, "admin");

        assertThat(event.getDeletedAt()).isNotNull();
        verify(entryRepository, never()).delete(any());
    }

    @Test
    void deleteEntryRefuseEtNeSupprimeRienSiLUtilisateurNEstNiCreateurNiAdmin() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        User autre = UserTestBuilder.aUser().withLogin("bob").build();
        Event event = new Event(new EventDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("bob")).thenReturn(Optional.of(autre));

        assertThatThrownBy(() -> service().deleteEntry(5L, "bob"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(event.getDeletedAt()).isNull();
        verify(entryRepository, never()).delete(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void findByIdDelegueAuRepository() {
        Event event = new Event(new EventDTO(), UserTestBuilder.aUser().build());
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThat(service().findById(5L)).isSameAs(event);
    }

    @Test
    void cancelOccurrenceAjouteLaDateAuxExDatesDeLaRecurrenceEtPubliePEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        Event event = new Event(dto, creator);
        LocalDate date = LocalDate.of(2026, 9, 20);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        service().cancelOccurrence(5L, date, "alice");

        assertThat(event.getRecurrence().getExDates()).containsExactly(date);
        verify(publisher).publishEvent(any(com.nyxeira.homerunner.entries.events.OccurrenceCancelledEvent.class));
    }

    @Test
    void retrieveOccurrenceRetireLaDateDesExDatesEtPubliePEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(1);
        Event event = new Event(dto, creator);
        LocalDate date = LocalDate.of(2026, 9, 20);
        event.getRecurrence().addExDate(date);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));

        service().retrieveOccurrence(5L, date, "alice");

        assertThat(event.getRecurrence().getExDates()).doesNotContain(date);
        verify(publisher).publishEvent(any(com.nyxeira.homerunner.entries.events.OccurrenceUpdatedEvent.class));
    }

    @Test
    void updateEventOccurrenceMaterialiseUneNouvelleOccurrenceQuandAucuneNExisteEncore() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        EventDTO initial = new EventDTO();
        initial.setName("Reunion");
        initial.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        initial.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        initial.setFrequency(Frequency.WEEKLY);
        initial.setInterval(1);
        Event event = new Event(initial, creator);
        LocalDate date = LocalDate.of(2026, 9, 8);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(event));
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(creator));
        when(eventOccurrenceRepository.findByMasterIdAndDate(5L, date)).thenReturn(Optional.empty());
        when(eventOccurrenceRepository.save(any(EventOccurrence.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDTO override = new EventDTO();
        override.setName("Reunion (exceptionnellement en visio)");
        override.setEndDate(LocalDateTime.of(2026, 9, 8, 12, 0));

        service().updateEventOccurrence(5L, date, override, "alice");

        ArgumentCaptor<EventOccurrence> captor = ArgumentCaptor.forClass(EventOccurrence.class);
        verify(eventOccurrenceRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Reunion (exceptionnellement en visio)");
        assertThat(captor.getValue().getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 8, 12, 0));
        verify(publisher).publishEvent(any(com.nyxeira.homerunner.entries.events.OccurrenceUpdatedEvent.class));
    }

    @Test
    void updateEventOccurrenceRefuseSiLEntreeNEstPasUnEvent() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Task task = new Task(new TaskDTO(), creator);
        when(entryRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service().updateEventOccurrence(5L, LocalDate.of(2026, 9, 8), new EventDTO(), "alice"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveOccurrenceRenvoieLOccurrenceMaterialiseeDUnEventSiElleExiste() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Event event = new Event(new EventDTO(), creator);
        LocalDate date = LocalDate.of(2026, 9, 8);
        EventOccurrence materialized = new EventOccurrence(event, date);
        // event.getId() est null tant que l'entite n'a jamais ete persistee (@GeneratedValue) :
        // on cale le mock sur cette meme cle plutot que de bricoler l'id par reflexion.
        when(eventOccurrenceRepository.findByMasterIdAndDate(event.getId(), date)).thenReturn(Optional.of(materialized));

        Occurrence resolved = service().resolveOccurrence(event, date);

        assertThat(resolved).isSameAs(materialized);
    }

    @Test
    void resolveOccurrenceConstruitUneOccurrenceTransitoireDUneTacheSiRienNEstMaterialise() {
        User creator = UserTestBuilder.aUser().withLogin("alice").build();
        Task task = new Task(new TaskDTO(), creator);
        LocalDate date = LocalDate.of(2026, 9, 8);
        when(taskOccurrenceRepository.findByMasterIdAndDate(null, date)).thenReturn(Optional.empty());

        Occurrence resolved = service().resolveOccurrence(task, date);

        assertThat(resolved).isInstanceOf(TaskOccurrence.class);
        assertThat(resolved.getDate()).isEqualTo(date);
        verify(taskOccurrenceRepository, never()).save(any());
    }
}
