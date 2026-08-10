package com.nyxeira.homerunner.usermanagement.model;

import jakarta.persistence.*;


@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    String login;

    String email;

    String passwordHash;

    boolean mustChangePassword;

    @Enumerated(EnumType.STRING)
    UserRole role;

    public User() {}

    public User(String login, String email, String passwordHash) {
        this.login = login;
        this.email = email;
        this.passwordHash = passwordHash;
    }


    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return login != null && login.equals(other.login);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
