package com.nyxeira.homerunner.entrymodel.model;


import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

@DiscriminatorValue("TASK")
@Entity
public class Task extends Entry {

    boolean validatedByOther;

    @ManyToMany
    @JoinTable(name="task_assignees")
    private Set<User> assignees = new HashSet<>();

    @ManyToMany
    @JoinTable(name="task_contributors")
    private Set<User> contributors = new HashSet<>();

    public Task() {}

    public boolean isValidatedByOther() { return validatedByOther; }

    public Set<User> getAssignees() { return assignees; }
    public void setAssignees(Set<User> assignees) { this.assignees = assignees; }
    public void addAssignee(User assignee) { this.assignees.add(assignee); }
    public void removeAssignee(User assignee) { this.assignees.remove(assignee); }

    public Set<User> getContributors() { return contributors; }


    public EntryType getType() { return EntryType.TASK; }
}
