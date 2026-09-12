package com.nyxeira.homerunner.entries.model.occurrences;

import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.usermanagement.model.User;

import java.time.LocalDate;
import java.util.Set;


public interface Occurrence {

    Entry getMaster();
    LocalDate getDate();
    String getName();
    String getDescription();
    boolean isParticipant(User user);
    void addParticipant(User user);
    void removeParticipant(User user);
    Set<User> getParticipants();

    void applyOverrides(EntryDTO d);
}
