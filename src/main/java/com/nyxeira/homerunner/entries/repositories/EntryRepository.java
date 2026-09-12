package com.nyxeira.homerunner.entries.repositories;

import com.nyxeira.homerunner.entries.model.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    @Query("""
    select e from Entry e
    where (e.date between :start and :end)
       or (e.recurrence is not null and (e.recurrence.until is null or e.recurrence.until >= :periodStart))
    """)
    List<Entry> findInPeriodOrRecurring(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("periodStart") LocalDate periodStart);

    // Utilisé par ReminderScheduler pour déplier la fenêtre glissante des occurrences futures
    // (cf. conception 5.8) : toute entrée récurrente encore active qui demande un rappel.
    @Query("""
    select e from Entry e
    where e.recurrence is not null
      and e.reminderMinutesBefore is not null
      and (e.recurrence.until is null or e.recurrence.until >= :today)
    """)
    List<Entry> findRecurringWithReminder(@Param("today") LocalDate today);
}