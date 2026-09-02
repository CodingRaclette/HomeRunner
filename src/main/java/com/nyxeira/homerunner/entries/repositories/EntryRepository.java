package com.nyxeira.homerunner.entries.repositories;

import com.nyxeira.homerunner.entries.model.Entry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {
}