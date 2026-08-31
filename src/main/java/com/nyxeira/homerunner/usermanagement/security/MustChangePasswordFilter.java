package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.common.web.WebPaths;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
// OncePerRequestFilter garantit une seule exécution par requête entrante, même si elle
// est ensuite redirigée/transférée en interne (évite les doubles vérifications)
public class MustChangePasswordFilter extends OncePerRequestFilter {

    public static final String STATIC_CSS = "/css/**";
    public static final String STATIC_JS = "/js/**";
    public static final String WEBJARS = "/webjars/**";
    public static final String H2_CONSOLE = "/h2-console/**";

    private boolean isExcludedResource(String resource) {
        return resource.startsWith("/webjars/") || resource.startsWith("/css/") || resource.startsWith("/js/");
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {

        // A ce stade de la chaîne, Spring Security a déjà placé l'Authentication de l'utilisateur courant dans
        // le SecurityContextHolder (soit tout juste authentifié, soit restauré depuis la session HTTP)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // On vérifie ici si il faut rediriger l'utilisateur vers la page de changement de mdp
        if (auth != null
                && auth.getPrincipal() instanceof UserPrincipal principal
                && principal.getUser().isMustChangePassword()
                && !req.getRequestURI().equals(WebPaths.CHANGE_PASSWORD_FULL) // évite la boucle infinie
                && !req.getRequestURI().equals(WebPaths.LOGOUT) // évite également la boucle infinie
                && !isExcludedResource(req.getRequestURI()))
        {
            // Si toutes les conditions sont remplies, on redirige l'utilisateur vers le changement de MDP
            res.sendRedirect(WebPaths.CHANGE_PASSWORD_FULL);
        } else {
            chain.doFilter(req, res); // Cas normal : rien à faire, on laisse la requête continuer son chemin
        }



    }
}