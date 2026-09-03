package com.nyxeira.homerunner.usermanagement.ui;

import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import com.nyxeira.homerunner.usermanagement.services.UserManagementService;

import com.nyxeira.homerunner.usermanagement.services.exceptions.EmailAlreadyUsedException;
import com.nyxeira.homerunner.usermanagement.services.exceptions.LoginAlreadyUsedException;
import com.nyxeira.homerunner.usermanagement.ui.forms.CreateUserForm;
import com.nyxeira.homerunner.usermanagement.ui.forms.ResetPasswordForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin")
public class AdminController {


    private final UserManagementService userManagementService;
    private final UserRepository userRepository;

    private final String FORM_PATH = "admin/user-form";


    public AdminController(UserManagementService userManagementService, UserRepository userRepository) {
        this.userManagementService = userManagementService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("userlist", userRepository.findAll());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("form", new CreateUserForm());
        return FORM_PATH;
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("form") CreateUserForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return FORM_PATH;
        }
        try {
            userManagementService.createUser(form.toDTO());
        } catch (LoginAlreadyUsedException e) {
            bindingResult.rejectValue("login", "invalid", "Le login renseigné existe déjà.");
            return FORM_PATH;
        } catch (EmailAlreadyUsedException e) {
            bindingResult.rejectValue("email", "invalid", "Le mail renseigné existe déjà.");
            return FORM_PATH;
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@Valid @ModelAttribute("form") ResetPasswordForm form, BindingResult bindingResult,
                                @PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Pas de vue dediee pour reafficher les erreurs de cette modale (elle vit dans admin/users.html,
        // qui n'a pas de BindingResult a montrer) : la validation cote client (reset-password-validation.js)
        // est le rempart principal, ce controle serveur n'est qu'un filet de securite. En cas d'echec on se
        // contente donc d'un message d'erreur generique en flash, sans rouvrir la modale ni conserver la saisie.
        if (bindingResult.hasErrors() || !form.getNewPassword().equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Le mot de passe n'a pas pu être réinitialisé : vérifiez qu'il fait au moins 8 caractères et que les deux saisies correspondent.");
            return "redirect:/admin/users";
        }
        userManagementService.resetPassword(id, form.getNewPassword());
        redirectAttributes.addFlashAttribute("successMessage", "Le mot de passe a été réinitialisé.");
        return "redirect:/admin/users";
    }

}
