package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.usermanagement.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

/**
 * SecurityContextHolder s'appuie sur un ThreadLocal : on nettoie systématiquement
 * après chaque test pour ne pas laisser fuiter un contexte d'authentification
 * vers un autre test exécuté sur le même thread.
 */
class AuthenticationRefresherTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void remplaceLAuthenticationCouranteParUnPrincipalAJour() {
        User updatedUser = UserTestBuilder.aUser().withPasswordHash("nouveau-hash").build();
        updatedUser.setMustChangePassword(false);

        AuthenticationRefresher.refresh(updatedUser);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getUser()).isSameAs(updatedUser);
        assertThat(principal.getUser().isMustChangePassword()).isFalse();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MEMBER");
    }
}
