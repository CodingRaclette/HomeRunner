package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.usermanagement.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Rafraîchit le principal Spring Security de la session courante après une mutation d'un User qui affecte des
 * informations mises en cache (ex : mustChangePassword).
 */
public final class AuthenticationRefresher {

    private AuthenticationRefresher() {} // classe utilitaire, n'a pas pour but d'être instancié

    public static void refresh(User updatedUser) {
        UserPrincipal principal = new UserPrincipal(updatedUser);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

}
