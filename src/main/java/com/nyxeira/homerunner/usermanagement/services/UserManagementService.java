package com.nyxeira.homerunner.usermanagement.services;


import com.nyxeira.homerunner.usermanagement.dto.CreateUserDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import com.nyxeira.homerunner.usermanagement.services.exceptions.EmailAlreadyUsedException;
import com.nyxeira.homerunner.usermanagement.services.exceptions.InvalidCurrentPasswordException;
import com.nyxeira.homerunner.usermanagement.services.exceptions.LoginAlreadyUsedException;
import jakarta.transaction.Transactional;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User changePassword(String login, String oldPassword, String newPassword) {
        // On récupère le user dans le repository
        User user = userRepository.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException(login));
        // On vérifie si le mot de passe actuel correspond à celui renseigné
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        // todo : On pourra ici ajouter une vérification que l'ancien mdp n'est pas égal au nouveau.

        // alors on chnge le mot de passe et on indique que ce user n'a plus besoin de changer son mdp
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);

        // On retourne l'utilisateur mis à jour pour la session courante
        return user;
    }

    @Transactional
    public User createUser(CreateUserDTO c) {
        // verif du login
        if (userRepository.existsByLogin(c.getLogin())) {
            throw new LoginAlreadyUsedException();
        }
        if (userRepository.existsByEmail(c.getEmail())) {
            throw new EmailAlreadyUsedException();
        }
        String passwordHash = passwordEncoder.encode(c.getPassword());

        User user = new User(c.getLogin(), c.getEmail(), c.getName(), passwordHash, UserRole.MEMBER);
        user.setMustChangePassword(true);
        userRepository.save(user);
        return user;
    }

}
