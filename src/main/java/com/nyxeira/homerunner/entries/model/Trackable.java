package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.usermanagement.model.User;

import java.util.Set;

public interface Trackable {

    boolean isParticipant(User user);
    void addParticipant(User user);
    void removeParticipant(User user);
    Set<User> getParticipants();

    Set<User> getContributors();
    void addContributor(User contributor);
    void removeContributor(User contributor);
    boolean isContributor(User user);
    void setValidatedByOther(boolean b);
    boolean isValidatedByOther();

    default boolean isDone() { return isValidatedByOther() || !getContributors().isEmpty(); }

}
