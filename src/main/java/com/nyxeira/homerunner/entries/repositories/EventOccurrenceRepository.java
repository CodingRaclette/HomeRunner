package com.nyxeira.homerunner.entries.repositories;

import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventOccurrenceRepository extends JpaRepository<EventOccurrence, Long> {

    Optional<EventOccurrence> findByMasterIdAndDate(Long masterId, LocalDate date);
    List<EventOccurrence> findByMasterIdInAndDateBetween(List<Long> masterIds, LocalDate start, LocalDate end);

}
