package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.events.EntryCreatedEvent;
import com.nyxeira.homerunner.entries.events.EntryUpdatedEvent;
import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.entries.dto.EntryFormDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EntryLifeService {

    private final EntryRepository entryRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public EntryLifeService(EntryRepository entryRepository,
                            UserRepository userRepository,
                            ApplicationEventPublisher publisher) {
        this.entryRepository = entryRepository;
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
        }
    }
}
