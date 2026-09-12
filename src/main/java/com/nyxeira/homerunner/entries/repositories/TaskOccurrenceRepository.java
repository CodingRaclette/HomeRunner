package com.nyxeira.homerunner.entries.repositories;

import com.nyxeira.homerunner.entries.model.occurrences.TaskOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskOccurrenceRepository extends JpaRepository<TaskOccurrence, Long> {

    Optional<TaskOccurrence> findByMasterIdAndDate(Long masterId, LocalDate date);
    List<TaskOccurrence> findByMasterIdInAndDateBetween(List<Long> masterIds, LocalDate start, LocalDate end);
}
