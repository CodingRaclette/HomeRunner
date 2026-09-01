package com.nyxeira.homerunner.usermanagement.ui;

import com.nyxeira.homerunner.common.web.WebPaths;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import com.nyxeira.homerunner.usermanagement.services.UserManagementService;

import com.nyxeira.homerunner.usermanagement.services.exceptions.EmailAlreadyUsedException;
import com.nyxeira.homerunner.usermanagement.services.exceptions.LoginAlreadyUsedException;
import com.nyxeira.homerunner.usermanagement.ui.forms.CreateUserForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


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


}
