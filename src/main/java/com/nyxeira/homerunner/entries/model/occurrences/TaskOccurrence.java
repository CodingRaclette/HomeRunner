package com.nyxeira.homerunner.entries.model.occurrences;

import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.Trackable;
import com.nyxeira.homerunner.usermanagement.model.User;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"master_id", "date"}))
@AssociationOverride(name = "participants", joinTable = @JoinTable(name = "task_occurrence_participants"))
public class TaskOccurrence extends AbstractOccurrence implements Trackable {

    @ManyToOne
    Task master;

    boolean validatedByOther;

    @ManyToMany
    @JoinTable(name = "task_occurrence_contributors")
    Set<User> contributors = new HashSet<>();

    public TaskOccurrence() {}

    public TaskOccurrence(Task master, LocalDate occurrenceDate) {
        this.master = master;
        this.date = occurrenceDate;
    }

    @Override
    public Entry getMaster() { return this.master; }


    @Override
    public Set<User> getContributors() {
        // les contributeurs sont forcéments propres à chaque occurrences, contrairement aux participants.
        return this.contributors;
    }


    @Override
    public void addContributor(User contributor) { this.contributors.add(contributor); }

    @Override
    public void removeContributor(User contributor) { this.contributors.remove(contributor); }

    @Override
    public boolean isContributor(User user) { return this.contributors.contains(user); }

    @Override
    public void setValidatedByOther(boolean b) { this.validatedByOther = b; }
    @Override
    public boolean isValidatedByOther() { return this.validatedByOther; }
}
