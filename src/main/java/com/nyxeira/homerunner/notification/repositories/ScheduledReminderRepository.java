package com.nyxeira.homerunner.notification.repositories;

import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.notification.model.ScheduledReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledReminderRepository extends JpaRepository<ScheduledReminder, Long> {

    List<ScheduledReminder> findBySentFalseAndFireAtLessThanEqual(LocalDateTime now);

    boolean existsByEntryAndOccurrenceDate(Entry entry, LocalDate occurrenceDate);

    // Suppression totale des rappels d'une entrée (entrée supprimée : cf. EntryDeletedEvent).
    void deleteByEntry(Entry entry);

    // occurrenceDate == null cible le rappel unique d'une entrée non récurrente (Spring Data
    // traduit un paramètre null en "is null" pour une comparaison d'égalité).
    void deleteByEntryAndOccurrenceDate(Entry entry, LocalDate occurrenceDate);

    // Rappels d'occurrences futures non encore envoyés : purgés quand la règle de récurrence
    // change (cf. NotificationService.onEntryUpdated), ReminderScheduler les recrée au besoin.
    void deleteByEntryAndOccurrenceDateIsNotNullAndSentFalse(Entry entry);
}
