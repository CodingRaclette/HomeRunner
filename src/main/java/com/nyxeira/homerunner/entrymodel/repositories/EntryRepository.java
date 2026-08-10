package com.nyxeira.homerunner.entrymodel.repositories;

import com.nyxeira.homerunner.entrymodel.model.Entry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {
}