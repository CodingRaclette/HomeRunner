package com.nyxeira.homerunner.usermanagement.ui.forms;


import com.nyxeira.homerunner.usermanagement.dto.CreateUserDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
Formulaire de création d'un utilisateur, pour un admin ou une personne autorisée.
 */
public class CreateUserForm {

    @NotNull
    String login;

    @Size(min = 6, max = 50) // 6 : a@b.fr
    @Email
    @NotNull
    String email;

    String name;

    // Pas besoin de grosse vérification ici : le MDP temp sera créé par l'admin mais sera changé à la première
    // connexion par l'utilisateur, avec les règles solides
    @NotNull
    @Size(min = 6, max = 50)
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

//    public void setEmail(String email) {
//        this.email = email;
//    }

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
