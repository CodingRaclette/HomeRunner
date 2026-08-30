package com.nyxeira.homerunner.entrylife.services;

import com.nyxeira.homerunner.entrylife.dto.EventDTO;
import com.nyxeira.homerunner.entrylife.dto.TaskDTO;
import com.nyxeira.homerunner.entrymodel.model.Entry;
import com.nyxeira.homerunner.entrymodel.model.Event;
import com.nyxeira.homerunner.entrymodel.model.Task;
import com.nyxeira.homerunner.entrymodel.repositories.EntryRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EntryLifeService {

    private final EntryRepository entryRepository;
    private final UserRepository userRepository;

    public EntryLifeService(EntryRepository entryRepository, UserRepository userRepository) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long createEvent(EventDTO d, String login) {
        User creator = userRepository.findByLogin(login).orElseThrow();
        Event event = new Event(d, creator);
        event.setParticipants(resolveUsers(d.getParticipantIds()));
        entryRepository.save(event);
        return event.getId();
    }

    @Transactional
    public Long createTask(TaskDTO d, String login) {
        User creator = userRepository.findByLogin(login).orElseThrow();
        Task task = new Task(d, creator);
        task.setAssignees(resolveUsers(d.getAssigneeIds()));
        entryRepository.save(task);
        return task.getId();
    }

    private Set<User> resolveUsers(List<Long> participantIds) {
        return participantIds == null ? Set.of() : new HashSet<>(userRepository.findAllById(participantIds));
    }


    public Entry findById(Long id) {
        return entryRepository.findById(id).orElseThrow();
    }
}
