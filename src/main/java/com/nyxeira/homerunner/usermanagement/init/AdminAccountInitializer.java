package com.nyxeira.homerunner.usermanagement.init;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);


    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) return; // Cette méthode ne se lance que si l'admin n'existe pas (au premier démarrage)

        String rawPassword = System.getenv("HOMERUNNER_ADMIN_PASSWORD");
        boolean generated = (rawPassword == null || rawPassword.isBlank());
        if (generated) {
            rawPassword = UUID.randomUUID().toString().substring(0, 12);
        }

        User admin = new User("admin", "admin@homerunner.local", "Administrateur",
                passwordEncoder.encode(rawPassword), UserRole.ADMIN);
        admin.setMustChangePassword(true);
        userRepository.save(admin);

        if (generated) {
            log.info("Compte admin créé. Mot de passe généré : {}", rawPassword);
        }
    }

}
