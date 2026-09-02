package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.usermanagement.model.User;

import java.util.Set;

public interface Trackable {

    boolean isDone();
    Set<User> getContributors();
    void addContributor(User contributor);
    void removeContributor(User contributor);
    boolean isContributor(User user);
    void setValidatedByOther(boolean b);

}
