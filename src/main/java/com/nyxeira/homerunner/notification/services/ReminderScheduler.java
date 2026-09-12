package com.nyxeira.homerunner.notification.services;

import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import com.nyxeira.homerunner.notification.model.Notification;
import com.nyxeira.homerunner.notification.model.ScheduledReminder;
import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.notification.repositories.ScheduledReminderRepository;
import com.nyxeira.homerunner.usermanagement.model.User;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Balaie périodiquement les ScheduledReminder en base : ils survivent à un redémarrage du
 * serveur (cf. conception 4.8/5.5), contrairement à un scheduling purement en mémoire.
 * <p>
 * Gère aussi le dépliage en fenêtre glissante des occurrences récurrentes (cf. 5.8) : une
 * occurrence non matérialisée n'a pas d'entité en base, donc pas de ScheduledReminder tant
 * qu'on ne l'a pas déplié explicitement.
 */
@Component
public class ReminderScheduler {

    // Horizon du dépliage nocturne : au-delà, les rappels seront matérialisés lors d'un passage
    // ultérieur, bien avant leur échéance réelle.
    private static final long WINDOW_DAYS = 14;

    private final EntryRepository entryRepository;
    private final EventOccurrenceRepository eventOccurrenceRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final ScheduledReminderRepository scheduledReminderRepository;
    private final NotificationRepository notificationRepository;

    public ReminderScheduler(EntryRepository entryRepository,
                              EventOccurrenceRepository eventOccurrenceRepository,
                              TaskOccurrenceRepository taskOccurrenceRepository,
                              ScheduledReminderRepository scheduledReminderRepository,
                              NotificationRepository notificationRepository) {
        this.entryRepository = entryRepository;
        this.eventOccurrenceRepository = eventOccurrenceRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
        this.scheduledReminderRepository = scheduledReminderRepository;
        this.notificationRepository = notificationRepository;
    }

    // Toutes les nuits à 2h : déplie les entrées récurrentes sur les WINDOW_DAYS prochains jours
    // et crée les ScheduledReminder manquants (cf. 5.8).
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void materializeUpcomingReminders() {
        LocalDate today = LocalDate.now();
        LocalDateTime windowStart = LocalDateTime.now();
        LocalDateTime windowEnd = windowStart.plusDays(WINDOW_DAYS);

        for (Entry entry : entryRepository.findRecurringWithReminder(today)) {
            for (LocalDateTime occurrence : entry.getRecurrence().occurrencesBetween(entry.getDate(), windowStart, windowEnd)) {
                LocalDate date = occurrence.toLocalDate();
                if (isMaterialized(entry, date)) {
                    // Porte déjà son propre rappel, créé par NotificationService.onOccurrenceUpdated.
                    continue;
                }
                if (!scheduledReminderRepository.existsByEntryAndOccurrenceDate(entry, date)) {
                    scheduledReminderRepository.save(new ScheduledReminder(entry,
                            occurrence.minusMinutes(entry.getReminderMinutesBefore()), date));
                }
            }
        }
    }

    private boolean isMaterialized(Entry entry, LocalDate date) {
        if (entry instanceof Event) {
            return eventOccurrenceRepository.findByMasterIdAndDate(entry.getId(), date).isPresent();
        } else if (entry instanceof Task) {
            return taskOccurrenceRepository.findByMasterIdAndDate(entry.getId(), date).isPresent();
        }
        return false;
    }

    // Toutes les minutes : envoie (sous forme de Notification) les rappels arrivés à échéance.
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void pollAndSend() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledReminder> due = scheduledReminderRepository.findBySentFalseAndFireAtLessThanEqual(now);
        for (ScheduledReminder reminder : due) {
            Entry entry = reminder.getEntry();
            String suffix = reminder.getOccurrenceDate() != null ? " (" + reminder.getOccurrenceDate() + ")" : "";
            String message = "Rappel : \"" + entry.getName() + "\"" + suffix + " approche";
            String link = "/entries/" + entry.getId()
                    + (reminder.getOccurrenceDate() != null ? "?date=" + reminder.getOccurrenceDate() : "");
            for (User recipient : entry.getParticipants()) {
                notificationRepository.save(new Notification(recipient, message, link));
            }
            reminder.markAsSent();
        }
    }
}
