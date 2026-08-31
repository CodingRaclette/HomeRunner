package com.nyxeira.homerunner.usermanagement.ui.forms;


import com.nyxeira.homerunner.usermanagement.dto.CreateUserDTO;

/*
Formulaire de création d'un utilisateur, pour un admin ou une personne autorisée.
 */
public class CreateUserForm {

    String login;
    String email;
    String name;
    String password;

    public CreateUserDTO toDTO() {
        CreateUserDTO u = new CreateUserDTO();
        u.setLogin(login);
        u.setEmail(email);
        u.setName(name);
        u.setPassword(password);
        return u;
    }


    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
