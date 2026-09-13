package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.usermanagement.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

/**
 * doFilterInternal est protected sur OncePerRequestFilter : on l'appelle
 * directement depuis un test du même package, sans passer par un vrai
 * conteneur servlet.
 */
@ExtendWith(MockitoExtension.class)
class MustChangePasswordFilterTest {

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    private final MustChangePasswordFilter filter = new MustChangePasswordFilter();
    private final String pathChangePassword = "/account/change-password";


    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void laisseContinuerSiPersonneNestConnectee() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void laisseContinuerSiMustChangePasswordEstFaux() throws Exception {
        User user = UserTestBuilder.aUser().withEmail("a@homerunner.local").build();
        user.setMustChangePassword(false);
        authenticateAs(user);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rediredigeVersChangePasswordSiLeDrapeauEstActif() throws Exception {
        User user = UserTestBuilder.aUser().withEmail("a@homerunner.local").build();
        user.setMustChangePassword(true);
        authenticateAs(user);
        when(request.getRequestURI()).thenReturn("/calendar");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendRedirect(pathChangePassword);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void nEvitePasLaBoucleSurLaPageDeChangementElleMeme() throws Exception {
        User user = UserTestBuilder.aUser().withEmail("a@homerunner.local").build();
        user.setMustChangePassword(true);
        authenticateAs(user);
        when(request.getRequestURI()).thenReturn(pathChangePassword);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void laisseToujoursPasserLeLogout() throws Exception {
        User user = UserTestBuilder.aUser().withEmail("a@homerunner.local").build();
        user.setMustChangePassword(true);
        authenticateAs(user);
        when(request.getRequestURI()).thenReturn("/logout");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
