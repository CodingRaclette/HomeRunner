package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.events.*;
import com.nyxeira.homerunner.entries.exceptions.UserNotParticipantException;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.Trackable;
import com.nyxeira.homerunner.entries.model.occurrences.TaskOccurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TaskTrackingService {

    private final EntryRepository entryRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public TaskTrackingService(EntryRepository entryRepository, TaskOccurrenceRepository taskOccurrenceRepository, UserRepository userRepository, ApplicationEventPublisher publisher) {
        this.entryRepository = entryRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
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

    private Trackable resolveTrackable(Long taskId) {
        return resolveTrackable(taskId, null);
    }

    private Trackable resolveTrackable(Long taskId, LocalDate date) {
        Task master = getTask(taskId);
        if (date == null) {
            return master; // Si il n'y a pas de date, on retourne la master
        } else {
            // Sinon, on récupère/crée l'occurrence à la date concernée
            return taskOccurrenceRepository.findByMasterIdAndDate(taskId, date)
                    .orElseGet(() -> taskOccurrenceRepository.save(new TaskOccurrence(master, date)));
        }
    }


    @Transactional
    public void selfAssign(Long taskId, LocalDate date,  String actorLogin) {
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        Trackable task = resolveTrackable(taskId, date);
        task.addParticipant(user);
        publisher.publishEvent(new SelfAssignEvent(taskId, date, actorLogin));
    }

    @Transactional
    public void selfAssign(Long taskId, String actorLogin) {
        selfAssign(taskId, null, actorLogin);
    }

    // Symetrique de selfAssign : tout assigne peut se retirer lui-meme, aucun controle de droit non plus.
    @Transactional
    public void selfUnassign(Long taskId, LocalDate date, String actorLogin) {
        Trackable task = resolveTrackable(taskId, date);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        task.removeParticipant(user);
        publisher.publishEvent(new SelfUnassignEvent(taskId, date, actorLogin));
    }

    @Transactional
    public void selfUnassign(Long taskId, String actorLogin) {
        selfUnassign(taskId, null, actorLogin);
    }


    @Transactional
    public void toggleContributor(Long taskId, LocalDate date, Long targetUserId, String actorLogin) {
        Trackable task = resolveTrackable(taskId, date);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();
        User targetUser = userRepository.findById(targetUserId).orElseThrow();

        Task master = getTask(taskId);
        if (!user.equals(targetUser) && !user.equals(master.getCreator())) {
            throw new AccessDeniedException("User can't edit this participant");
        }

        // L'utilisateur visé doit être un participant
        if (!task.getParticipants().contains(targetUser)) { throw new UserNotParticipantException(); }

        if (task.getContributors().contains(targetUser)) {
            // Cas ou l'utilsateur est déjà contributeur, on le retire des contributeur
            task.removeContributor(targetUser);
            publisher.publishEvent(new ContributionRemovedEvent(taskId, date, targetUser.getLogin(), actorLogin));
        } else {
            // Sinon on l'y ajoute
            task.addContributor(targetUser);
            publisher.publishEvent(new ContributionAddedEvent(taskId, date, targetUser.getLogin(), actorLogin));
        }
    }

    @Transactional
    public void toggleContributor(Long taskId, Long targetUserId, String actorLogin) {
        toggleContributor(taskId, null, targetUserId, actorLogin);
    }

    @Transactional
    public void toggleValidatedByOther(Long taskId, LocalDate date, String actorLogin) {
        Trackable task = resolveTrackable(taskId, date);
        User user = userRepository.findByLogin(actorLogin).orElseThrow();

        Task master = getTask(taskId);
        if (master.isEditableBy(user)) {
            task.setValidatedByOther(!task.isValidatedByOther());
            publisher.publishEvent(new TaskValidatedByOtherEvent(taskId, date, task.isValidatedByOther(), actorLogin));
        } else { throw new AccessDeniedException("User can't set this task done"); }
    }

    @Transactional
    public void toggleValidatedByOther(Long taskId, String actorLogin) {
        toggleValidatedByOther(taskId, null, actorLogin);
    }
}
