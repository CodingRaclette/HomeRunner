package com.nyxeira.homerunner.notification.services;

import com.nyxeira.homerunner.entries.events.*;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.occurrences.Occurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.notification.model.Notification;
import com.nyxeira.homerunner.notification.model.ScheduledReminder;
import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.notification.repositories.ScheduledReminderRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

/**
 * Écoute les événements applicatifs publiés par entrylife/tasktracking (pattern Observer,
 * cf. conception 5.5) et centralise la décision "qui notifier et comment". Ne dépend d'aucun
 * composant métier : uniquement des repositories/modèles partagés (Entry, User) et de son
 * propre modèle (Notification, ScheduledReminder).
 * <p>
 * Chaque écouteur se déclenche après commit (AFTER_COMMIT) : on ne notifie jamais une
 * opération qui a échoué.
 * <p>
 * Chaque écouteur est aussi explicitement REQUIRES_NEW : au moment où AFTER_COMMIT se déclenche,
 * la transaction d'origine vient de committer mais ses ressources (EntityManager/connexion) ne
 * sont pas encore complètement détachées du thread. Sans REQUIRES_NEW, un simple save() ici
 * rejoint silencieusement cette synchronisation finissante au lieu d'ouvrir une vraie transaction
 * : l'insert reste en attente de flush, personne ne le commite jamais, et la notification
 * n'atteint jamais la base (l'id généré reste null). REQUIRES_NEW force une transaction fraîche
 * et indépendante, qui committe pour de vrai à la fin de la méthode.
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final EntryRepository entryRepository;
    private final EventOccurrenceRepository eventOccurrenceRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ScheduledReminderRepository scheduledReminderRepository;

    public NotificationService(EntryRepository entryRepository,
                                EventOccurrenceRepository eventOccurrenceRepository,
                                TaskOccurrenceRepository taskOccurrenceRepository,
                                UserRepository userRepository,
                                NotificationRepository notificationRepository,
                                ScheduledReminderRepository scheduledReminderRepository) {
        this.entryRepository = entryRepository;
        this.eventOccurrenceRepository = eventOccurrenceRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.scheduledReminderRepository = scheduledReminderRepository;
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onEntryCreated(EntryCreatedEvent event) {
        entryRepository.findById(event.entryId()).ifPresent(entry -> {
            String link = link(entry, null);
            notify(entry.getParticipants(), event.actorLogin(),
                    entry.getCreator().getName() + " vous a ajouté à " + typeArticleLower(entry)
                            + " \"" + entry.getName() + "\" " + dateContext(entry), link);
            scheduleReminderIfNeeded(entry);
        });
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onEntryUpdated(EntryUpdatedEvent event) {
        entryRepository.findById(event.entryId()).ifPresent(entry -> {
            String link = link(entry, null);
            Set<Long> oldIds = new HashSet<>(event.oldParticipantIds());
            Set<Long> newIds = new HashSet<>(event.newParticipantIds());

            List<Long> addedIds = newIds.stream().filter(id -> !oldIds.contains(id)).toList();
            List<Long> removedIds = oldIds.stream().filter(id -> !newIds.contains(id)).toList();
            List<Long> keptIds = newIds.stream().filter(oldIds::contains).toList();

            String context = typeArticleLower(entry) + " \"" + entry.getName() + "\" " + dateContext(entry);
            notify(userRepository.findAllById(addedIds), event.actorLogin(),
                    "Vous avez été ajouté(e) à " + context, link);
            notify(userRepository.findAllById(removedIds), event.actorLogin(),
                    "Vous avez été retiré(e) de " + context, link);
            notify(userRepository.findAllById(keptIds), event.actorLogin(),
                    typeArticleUpper(entry) + " \"" + entry.getName() + "\" " + dateContext(entry) + " a été modifiée", link);

            // La date ou la règle de récurrence a pu changer : les rappels déjà programmés ne sont
            // plus forcément valides. Un rappel unique (entrée non récurrente) est recalculé tout
            // de suite ; pour une entrée récurrente, ReminderScheduler recrée les rappels
            // d'occurrence manquants à son prochain passage (cf. 5.8).
            scheduledReminderRepository.deleteByEntryAndOccurrenceDate(entry, null);
            scheduledReminderRepository.deleteByEntryAndOccurrenceDateIsNotNullAndSentFalse(entry);
            scheduleReminderIfNeeded(entry);
        });
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onEntryDeleted(EntryDeletedEvent event) {
        notify(userRepository.findAllById(event.participantIds()), event.actorLogin(),
                "\"" + event.entryName() + "\" a été supprimée", null);

        // L'entrée est déjà soft-supprimée : @SQLRestriction empêche de la recharger ici (cf.
        // Entry.java). getReferenceById ne fait qu'un proxy porteur de l'id, jamais initialisé
        // par la requête de suppression qui suit : cela contourne le filtre sans le désactiver.
        Entry entryRef = entryRepository.getReferenceById(event.entryId());
        scheduledReminderRepository.deleteByEntry(entryRef);
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onOccurrenceCancelled(OccurrenceCancelledEvent event) {
        entryRepository.findById(event.entryId()).ifPresent(entry -> {
            notify(entry.getParticipants(), event.login(),
                    "L'occurrence du " + DATE_FMT.format(event.date()) + " de " + typeArticleLower(entry)
                            + " \"" + entry.getName() + "\" a été annulée",
                    link(entry, null));
            scheduledReminderRepository.deleteByEntryAndOccurrenceDate(entry, event.date());
        });
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onOccurrenceUpdated(OccurrenceUpdatedEvent event) {
        entryRepository.findById(event.eventId()).ifPresent(entry -> {
            LocalDate date = event.date();
            notify(resolveOccurrenceParticipants(entry, date), event.login(),
                    "L'occurrence du " + DATE_FMT.format(date) + " de " + typeArticleLower(entry)
                            + " \"" + entry.getName() + "\" a été modifiée",
                    link(entry, date));

            // Une occurrence matérialisée porte son propre rappel, créé ici la première fois :
            // ReminderScheduler l'ignorera ensuite lors de son dépliage nocturne (cf. 5.8).
            if (entry.getReminderMinutesBefore() != null
                    && !scheduledReminderRepository.existsByEntryAndOccurrenceDate(entry, date)) {
                LocalDateTime occurrenceDateTime = date.atTime(entry.getDate().toLocalTime());
                scheduledReminderRepository.save(new ScheduledReminder(entry,
                        occurrenceDateTime.minusMinutes(entry.getReminderMinutesBefore()), date));
            }
        });
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onSelfAssigned(SelfAssignEvent event) {
        entryRepository.findById(event.taskId()).ifPresent(entry ->
                userRepository.findByLogin(event.login()).ifPresent(actor ->
                        notify(List.of(entry.getCreator()), event.login(),
                                actor.getName() + " s'est assigné(e) à la tâche \"" + entry.getName() + "\" "
                                        + dateContext(entry, event.date()),
                                link(entry, event.date()))));
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onSelfUnassigned(SelfUnassignEvent event) {
        entryRepository.findById(event.taskId()).ifPresent(entry ->
                userRepository.findByLogin(event.login()).ifPresent(actor ->
                        notify(List.of(entry.getCreator()), event.login(),
                                actor.getName() + " s'est désassigné(e) de la tâche \"" + entry.getName() + "\" "
                                        + dateContext(entry, event.date()),
                                link(entry, event.date()))));
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onContributionAdded(ContributionAddedEvent event) {
        entryRepository.findById(event.taskId()).ifPresent(entry ->
                userRepository.findByLogin(event.targetLogin()).ifPresent(target ->
                        notify(List.of(target), event.ActorLogin(),
                                "Vous avez été marqué(e) comme ayant contribué à la tâche \"" + entry.getName()
                                        + "\" " + dateContext(entry, event.date()),
                                link(entry, event.date()))));
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onContributionRemoved(ContributionRemovedEvent event) {
        entryRepository.findById(event.taskId()).ifPresent(entry ->
                userRepository.findByLogin(event.targetLogin()).ifPresent(target ->
                        notify(List.of(target), event.ActorLogin(),
                                "Vous avez été retiré(e) des contributeurs de la tâche \"" + entry.getName()
                                        + "\" " + dateContext(entry, event.date()),
                                link(entry, event.date()))));
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onTaskValidatedByOther(TaskValidatedByOtherEvent event) {
        Entry entry = entryRepository.findById(event.taskId()).orElse(null);
        if (!(entry instanceof Task task)) return;
        String verb = event.state() ? "validée manuellement" : "retirée de la validation manuelle";
        notify(task.getParticipants(), event.actorLogin(),
                "La tâche \"" + task.getName() + "\" " + dateContext(task, event.date()) + " a été " + verb,
                link(task, event.date()));
    }

    private void scheduleReminderIfNeeded(Entry entry) {
        // Les occurrences d'une entrée récurrente n'ont pas de date unique : leurs rappels sont
        // matérialisés à la volée par ReminderScheduler (fenêtre glissante, cf. 5.8) ou lors de
        // la matérialisation d'une occurrence (onOccurrenceUpdated) plutôt qu'ici.
        if (!entry.isRecurring() && entry.getReminderMinutesBefore() != null) {
            scheduledReminderRepository.save(new ScheduledReminder(entry,
                    entry.getDate().minusMinutes(entry.getReminderMinutesBefore()), null));
        }
    }

    private Set<User> resolveOccurrenceParticipants(Entry entry, LocalDate date) {
        if (entry instanceof Event) {
            return eventOccurrenceRepository.findByMasterIdAndDate(entry.getId(), date)
                    .map(Occurrence::getParticipants)
                    .orElseGet(entry::getParticipants);
        } else if (entry instanceof Task) {
            return taskOccurrenceRepository.findByMasterIdAndDate(entry.getId(), date)
                    .map(Occurrence::getParticipants)
                    .orElseGet(entry::getParticipants);
        }
        return entry.getParticipants();
    }

    private String typeArticleLower(Entry entry) {
        return entry.getType() == EntryType.EVENT ? "l'évènement" : "la tâche";
    }

    private String typeArticleUpper(Entry entry) {
        return entry.getType() == EntryType.EVENT ? "L'évènement" : "La tâche";
    }

    // Précise la période (évènement) ou l'échéance (tâche) concernée, pour que la notification
    // reste compréhensible même si le destinataire consulte plusieurs entrées du même nom.
    private String dateContext(Entry entry) {
        return dateContext(entry, null);
    }

    // Variante utilisée quand l'action porte sur une occurrence précise (ex : (dé)assignation sur
    // une occurrence) plutôt que sur l'entrée master : la date de l'occurrence prime alors sur
    // celle de l'entrée, sans quoi la notification afficherait la mauvaise échéance.
    private String dateContext(Entry entry, LocalDate occurrenceDate) {
        if (occurrenceDate != null) {
            return "(occurrence du " + DATE_FMT.format(occurrenceDate) + ")";
        }
        if (entry instanceof Event event) {
            // endDate est facultative (cf. EntryFormDTO.isEndDateValid) : un évènement peut n'avoir
            // qu'une heure de début.
            return event.getEndDate() != null
                    ? "du " + DATETIME_FMT.format(event.getDate()) + " au " + DATETIME_FMT.format(event.getEndDate())
                    : "le " + DATETIME_FMT.format(event.getDate());
        }
        return "prévue le " + DATETIME_FMT.format(entry.getDate());
    }

    private String link(Entry entry, LocalDate occurrenceDate) {
        return "/entries/" + entry.getId() + (occurrenceDate != null ? "?date=" + occurrenceDate : "");
    }

    // Notifie chaque destinataire sauf l'acteur à l'origine de l'action : centralise la règle
    // "notifier les participants sauf l'acteur" répétée dans l'analyse (cf. conception 5.5).
    private void notify(Collection<User> recipients, String actorLogin, String message, String link) {
        for (User recipient : recipients) {
            if (recipient.getLogin().equals(actorLogin)) continue;
            notificationRepository.save(new Notification(recipient, message, link));
        }
    }
}
