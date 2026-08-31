package com.nyxeira.homerunner.usermanagement.model;

/**
 * Test Data Builder pour User : centralise la construction des utilisateurs de test
 * avec des valeurs par defaut sensees. Quand User change (nouvel attribut, ordre du
 * constructeur...), un seul endroit a corriger au lieu de tous les tests.
 */
public class UserTestBuilder {

    private String login = "alice";
    private String email = "alice@homerunner.local";
    private String name = "Alice";
    private String passwordHash = "hash";
    private UserRole role = UserRole.MEMBER;
    private boolean mustChangePassword = false;

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withLogin(String login) {
        this.login = login;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserTestBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserTestBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserTestBuilder withMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
        return this;
    }

    public User build() {
        User user = new User(login, email, name, passwordHash, role);
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
