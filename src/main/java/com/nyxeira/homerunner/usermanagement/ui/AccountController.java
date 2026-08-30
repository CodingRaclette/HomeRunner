package com.nyxeira.homerunner.usermanagement.ui;


import com.nyxeira.homerunner.common.web.WebPaths;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.security.AuthenticationRefresher;

import com.nyxeira.homerunner.usermanagement.services.InvalidCurrentPasswordException;
import com.nyxeira.homerunner.usermanagement.services.UserManagementService;
import com.nyxeira.homerunner.usermanagement.ui.forms.ChangePasswordForm;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping(WebPaths.ACCOUNT)
public class AccountController {

    private final UserManagementService userManagementService;

    public AccountController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping(WebPaths.CHANGE_PASSWORD)
    public String getChangePasswordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return WebPaths.CHANGE_PASSWORD_FULL;
    }

    @PostMapping(WebPaths.CHANGE_PASSWORD)
    public String changePassword(@Valid @ModelAttribute("form") ChangePasswordForm form,
                                 BindingResult bindingResult, Principal principal) {
        // premier cas d'erreur : il y a des erreurs dans le formulaire
        if (bindingResult.hasErrors()) {
            return WebPaths.CHANGE_PASSWORD_FULL;
        }
        // deuxième cas d'erreur : le nouveau mdp et la confirmation ne sont pas identiques
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.passwords", "Le nouveau mot de passe et sa confirmation ne sont pas identiques.");
            return WebPaths.CHANGE_PASSWORD_FULL;
        }

        // todo : on pourra ici ajouter des contraintes sur la présence de caractères spéciaux et chiffres.

        // Si tout se passe bien, on essaie de changer le mdp:
        try {
            User updatedUser = userManagementService.changePassword(principal.getName(), form.getOldPassword(), form.getNewPassword());

            // On récupère le user mis à jour et on le met à jour dans le contexte de SpringBoot
            AuthenticationRefresher.refresh(updatedUser);

        } catch (InvalidCurrentPasswordException e) {
            bindingResult.rejectValue("oldPassword", "invalid", "Le mot de passe actuel n'est pas correct.");
            return WebPaths.CHANGE_PASSWORD_FULL;
        }
        return "redirect:/";

    }
}
