package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.events.*;
import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.entries.dto.EntryFormDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.entries.model.occurrences.Occurrence;
import com.nyxeira.homerunner.entries.model.occurrences.TaskOccurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EntryLifeService {

    private final EntryRepository entryRepository;
    private final EventOccurrenceRepository eventOccurrenceRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public EntryLifeService(EntryRepository entryRepository, EventOccurrenceRepository eventOccurrenceRepository,
                            TaskOccurrenceRepository taskOccurrenceRepository,
                            UserRepository userRepository, ApplicationEventPublisher publisher) {
        this.entryRepository = entryRepository;
        this.eventOccurrenceRepository = eventOccurrenceRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.userRepository = userRepository;
        this.publisher = publisher;
    }

    @Transactional
    public Long createEvent(EventDTO d, String login) {
        User creator = userRepository.findByLogin(login).orElseThrow();
        Event event = new Event(d, creator);
        event.setParticipants(resolveUsers(d.getParticipantIds()));
        entryRepository.save(event);
        publisher.publishEvent(new EntryCreatedEvent(event.getId(), login));
        return event.getId();
    }

    @Transactional
    public Long createTask(TaskDTO d, String login) {
        User creator = userRepository.findByLogin(login).orElseThrow();
        Task task = new Task(d, creator);
        task.setParticipants(resolveUsers(d.getParticipantIds()));
        entryRepository.save(task);
        publisher.publishEvent(new EntryCreatedEvent(task.getId(), login));
        return task.getId();
    }

    private Set<User> resolveUsers(List<Long> participantIds) {
        return participantIds == null ? Set.of() : new HashSet<>(userRepository.findAllById(participantIds));
    }

    public Entry findById(Long id) {
        return entryRepository.findById(id).orElseThrow();
    }

    public EntryFormDTO getEntryForEdit(Long entryId, String actorLogin) {
        Entry entry = entryRepository.findById(entryId).orElseThrow();
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        if (entry.isEditableBy(user)) {
            return new EntryFormDTO().fromEntry(entry);
        } else { throw new AccessDeniedException("User can't edit this entry"); }
    }

    @Transactional
    public void updateEntry(Long entryId, EntryDTO d, String actorLogin) {
        Entry entry = entryRepository.findById(entryId).orElseThrow();
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        if (entry.isEditableBy(user)) {
            List<Long> oldParticipants = entry.getParticipantIds();
            entry.applyData(d);
            entry.setParticipants(resolveUsers(d.getParticipantIds()));
            List<Long> newParticipants = entry.getParticipantIds();
            publisher.publishEvent(new EntryUpdatedEvent(entryId, oldParticipants, newParticipants, actorLogin));
        } else { throw new AccessDeniedException("User can't edit this entry"); }
    }

    @Transactional
    public void deleteEntry(Long entryId, String actorLogin) throws AccessDeniedException {
        Entry entry = entryRepository.findById(entryId).orElseThrow();
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        if (entry.isDeletableBy(user)) {
            entry.softDelete();
            publisher.publishEvent(new EntryDeletedEvent(entryId, actorLogin));
        } else { throw new AccessDeniedException("User can't delete this entry"); }
    }

    @Transactional
    public void cancelOccurrence(Long entryId, LocalDate date, String actorLogin) {
        Entry entry = entryRepository.findById(entryId).orElseThrow();
        User user = userRepository.findByLogin(actorLogin).orElseThrow();

        if (entry.isEditableBy(user)) {
            entry.getRecurrence().addExDate(date);
            publisher.publishEvent(new OccurrenceCancelledEvent(entryId, date, actorLogin));
        }
    }

    @Transactional
    public void retrieveOccurrence(Long entryId, LocalDate date, String actorLogin) {
        Entry entry = entryRepository.findById(entryId).orElseThrow();
        User user = userRepository.findByLogin(actorLogin).orElseThrow();

        if (entry.isEditableBy(user)) {
            entry.getRecurrence().removeExDate(date);
            publisher.publishEvent(new OccurrenceUpdatedEvent(entryId, date, actorLogin));
        }
    }

    @Transactional
    public void updateEventOccurrence(Long eventId, LocalDate date, EventDTO dto, String actorLogin) {
        Entry entry =  entryRepository.findById(eventId).orElseThrow();
        if (!(entry.getType() == EntryType.EVENT)) {
            throw new AccessDeniedException("L'entrée demandée n'est pas un event");
        }
        Event master = (Event)entry;
        User user = userRepository.findByLogin(actorLogin).orElseThrow();

        if (master.isEditableBy(user)) {
            EventOccurrence occurrence = eventOccurrenceRepository.findByMasterIdAndDate(eventId, date)
                    .orElseGet(() -> eventOccurrenceRepository.save(new EventOccurrence(master, date)));
            occurrence.applyOverrides(dto);
            publisher.publishEvent(new OccurrenceUpdatedEvent(eventId, date, actorLogin));
        }
    }

    // Pré-remplissage du formulaire d'édition d'une occurrence :
    // lecture seule, donc si l'occurrence n'est pas encore matérialisée on construit un EventOccurrence transitoire (jamais sauvegardé ici)
    // dont les getters retombent naturellement sur les valeurs de la master.
    public EntryFormDTO getEventOccurrenceForEdit(Long eventId, LocalDate date, String actorLogin) {
        Entry entry = entryRepository.findById(eventId).orElseThrow();
        if (!(entry instanceof Event master)) {
            throw new AccessDeniedException("L'entrée demandée n'est pas un event");
        }
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        if (!master.isEditableBy(user)) {
            throw new AccessDeniedException("User can't edit this entry");
        }
        EventOccurrence occurrence = eventOccurrenceRepository.findByMasterIdAndDate(eventId, date)
                .orElseGet(() -> new EventOccurrence(master, date));
        LocalDateTime occurrenceDateTime = date.atTime(master.getDate().toLocalTime());
        return new EntryFormDTO().fromEventOccurrence(occurrence, occurrenceDateTime);
    }

    // Résolution en lecture seule de l'occurrence (Event ou Task) à une date donnée, pour l'affichage de la page détail.
    // Rien n'est persisté si l'occurrence n'existe pas encore.
    public Occurrence resolveOccurrence(Entry entry, LocalDate date) {
        if (entry instanceof Event event) {
            return eventOccurrenceRepository.findByMasterIdAndDate(entry.getId(), date)
                    .<Occurrence>map(o -> o)
                    .orElseGet(() -> new EventOccurrence(event, date));
        } else if (entry instanceof Task task) {
            return taskOccurrenceRepository.findByMasterIdAndDate(entry.getId(), date)
                    .<Occurrence>map(o -> o)
                    .orElseGet(() -> new TaskOccurrence(task, date));
        }
        throw new IllegalStateException("Type d'entrée inconnu : " + entry.getType());
    }

}
