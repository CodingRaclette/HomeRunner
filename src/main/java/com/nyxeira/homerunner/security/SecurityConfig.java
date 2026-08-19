package com.nyxeira.homerunner.security;

import com.nyxeira.homerunner.usermanagement.security.MustChangePasswordFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.nyxeira.homerunner.common.web.WebPaths.*;


@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MustChangePasswordFilter mustChangePasswordFilter) {
        http
                .authorizeHttpRequests(auth -> auth
                        // On laisse d'abord passer les ressources statiques pour tout le monde avant d'interdire le reste aux non-connectés
                        .requestMatchers(STATIC_CSS, STATIC_JS, WEBJARS, H2_CONSOLE).permitAll()
                        .requestMatchers(ADMIN).hasRole("ADMIN")
                        .anyRequest().authenticated())

                // todo : Formulaire de connexion standard de SpringSecurity (/login), à faire évoluer plus tard
                .formLogin(Customizer.withDefaults())
                // active "/logout" qui déconnecte la session
                .logout(Customizer.withDefaults())

                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // La console H2 poste ses requêtes sans jeton CSRF (elle ne connaît pas Spring Security) ; sans
                // l'exception suivante, chaque requête est rejetée 403.
                .csrf(csrf -> csrf.ignoringRequestMatchers(H2_CONSOLE))
                // On insère notre filtre maison juste après celui qui gère l'authentification
                // par formulaire : ainsi, immédiatement après une connexion réussie, le contexte
                // de sécurité est déjà peuplé quand notre filtre s'exécute, et la redirection
                // vers le changement de mot de passe peut se déclencher sans aller-retour en plus.
                .addFilterAfter(mustChangePasswordFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}