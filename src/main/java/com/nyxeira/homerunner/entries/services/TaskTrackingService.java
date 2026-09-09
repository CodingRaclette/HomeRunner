package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.events.*;
import com.nyxeira.homerunner.entries.exceptions.UserNotParticipantException;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
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

    // Ouvert a tout membre authentifie
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


    @Transactional
    public void toggleContributor(Long taskId, Long targetUserId, String actorLogin) {
        Task task = getTask(taskId);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        User targetUser = userRepository.findById(targetUserId).orElseThrow();

        if (!user.equals(targetUser) && !user.equals(task.getCreator())) {
            throw new AccessDeniedException("User can't edit this participant");
        }

        // L'utilisateur visé doit être un participant
        if (!task.getParticipants().contains(targetUser)) { throw new UserNotParticipantException(); }

        if (task.getContributors().contains(targetUser)) {
            // Cas ou l'utilsateur est déjà contributeur, on le retire des contributeur
            task.removeContributor(targetUser);
            publisher.publishEvent(new ContributionRemovedEvent(taskId, targetUser.getLogin(), actorLogin));
        } else {
            // Sinon on l'y ajoute
            task.addContributor(targetUser);
            publisher.publishEvent(new ContributionAddedEvent(taskId, targetUser.getLogin(), actorLogin));
        }
    }

    @Transactional
    public void toggleValidatedByOther(Long taskId, String actorLogin) {
        Task task = getTask(taskId);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();

        if (task.isEditableBy(user)) {
            task.setValidatedByOther(!task.isValidatedByOther());
            publisher.publishEvent(new TaskValidatedByOtherEvent(taskId, task.isValidatedByOther(), actorLogin));
        } else { throw new AccessDeniedException("User can't set this task done"); }
    }
}
