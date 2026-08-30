package com.nyxeira.homerunner.entrymodel.model;


import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;


/**
 * Cette entité définit une tâche dans un calendrier. La tâche se distingue par le fait qu'elle :
 *  - possède un statut de validation, définit par la présence d'au moins un contributeur OU l'attribut validatedByOther ;
 *  - est associée à une liste de users assignés à la tâche (et qui peuvent donc la valider) ;
 *  - est associée à une liste de contributeurs, qui indique les users qui ont effectivement participé à l'accomplissement de la tâche.
 *  Je me questionne toujours sur la ressemblance entre "participants" de Event et "assignees", mais il me semble pour
 *  l'instant judicieux de les garder séparés, dans le cas où des comportements particuliers pourraient s'appliquer.
 *  A voir dans la suite du développement.
 */
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

    public Task(TaskDTO d, User creator) {
        this.name = d.getName();
        this.date = d.getDate();
        this.description = d.getDescription();
        this.creator = creator;
    }

    public boolean isValidatedByOther() { return validatedByOther; }

    public Set<User> getAssignees() { return assignees; }
    public void setAssignees(Set<User> assignees) { this.assignees = assignees; }
    public void addAssignee(User assignee) { this.assignees.add(assignee); }
    public void removeAssignee(User assignee) { this.assignees.remove(assignee); }

    public Set<User> getContributors() { return contributors; }


    public EntryType getType() { return EntryType.TASK; }
}
