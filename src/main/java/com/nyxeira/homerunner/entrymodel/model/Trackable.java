package com.nyxeira.homerunner.entrymodel.model;

import com.nyxeira.homerunner.usermanagement.model.User;

import java.util.Set;

public interface Trackable {

    boolean isDone();
    Set<User> getAssignees();
    void setAssignees(Set<User> assignees);
    Set<User> getContributors();
    void addContributor(User contributor);
    void removeContributor(User contributor);
    boolean isContributor(User user);
    void setValidatedByOther(boolean b);

}
