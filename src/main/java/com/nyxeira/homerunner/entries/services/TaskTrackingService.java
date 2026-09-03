package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.events.SelfAssignEvent;
import com.nyxeira.homerunner.entries.events.SelfUnassignEvent;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskTrackingService {

    private final EntryRepository entryRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public TaskTrackingService(EntryRepository entryRepository, UserRepository userRepository, ApplicationEventPublisher publisher) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.publisher = publisher;
    }

    private Task getTask(Long taskId) {
        Entry entry = entryRepository.findById(taskId).orElseThrow();
        if (!(entry instanceof Task task)) {
            // Filet de securite : les boutons "S'assigner"/"Se désassigner" ne sont affiches que dans
            // le bloc TASK de entries/detail.html, mais on se protege quand meme d'un appel direct
            // sur l'URL d'un Event.
            throw new IllegalArgumentException("L'entrée " + taskId + " n'est pas une tâche");
        }
        return task;
    }

    // Ouvert a tout membre authentifie, aucun controle de droit (cf. conception 6.5.1).
    @Transactional
    public void selfAssign(Long taskId, String actorLogin) {
        Task task = getTask(taskId);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        task.getParticipants().add(user);
        publisher.publishEvent(new SelfAssignEvent(taskId, actorLogin));
    }

    // Symetrique de selfAssign : tout assigne peut se retirer lui-meme, aucun controle de droit non plus.
    @Transactional
    public void selfUnassign(Long taskId, String actorLogin) {
        Task task = getTask(taskId);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        task.getParticipants().remove(user);
        publisher.publishEvent(new SelfUnassignEvent(taskId, actorLogin));
    }
}
