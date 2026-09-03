package com.nyxeira.homerunner.entries.model;


import com.nyxeira.homerunner.entries.dto.EntryDTO;
import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Cette entité définit une tâche dans un calendrier. La tâche se distingue par le fait qu'elle :
 *  - possède un statut de validation, définit par la présence d'au moins un contributeur OU l'attribut validatedByOther ;
 *  - est associée à une liste de users assignés à la tâche (et qui peuvent donc la valider) ;
 *  - est associée à une liste de contributeurs, qui indique les users qui ont effectivement participé à l'accomplissement de la tâche.
 */
@DiscriminatorValue("TASK")
@Entity
public class Task extends Entry implements Trackable {

    boolean validatedByOther;

    @ManyToMany
    @JoinTable(name="task_contributors")
    private final Set<User> contributors = new HashSet<>();


    public Task() {}

    public Task(TaskDTO d, User creator) {
        this.name = d.getName();
        this.date = d.getDate();
        this.description = d.getDescription();
        this.creator = creator;
    }

    public boolean isValidatedByOther() { return validatedByOther; }
    @Override
    public void setValidatedByOther(boolean b) { this.validatedByOther = b; }



    @Override
    public void applyData(EntryDTO dto) {
        TaskDTO taskDTO = (TaskDTO)dto;
        this.name = taskDTO.getName();
        this.date = taskDTO.getDate();
        this.description = taskDTO.getDescription();
    }

    @Override
    public boolean isDone() {
        return isValidatedByOther() | !getContributors().isEmpty();
    }

    public Set<User> getContributors() { return contributors; }
    public List<Long> getContributorIds() {
        return contributors.stream().map(User::getId).toList();
    }

    @Override
    public void addContributor(User contributor) { this.contributors.add(contributor); }
    @Override
    public void removeContributor(User contributor) { this.contributors.remove(contributor); }
    @Override
    public boolean isContributor(User user) { return this.contributors.contains(user); }

    public EntryType getType() { return EntryType.TASK; }
}
