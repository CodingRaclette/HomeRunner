package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

class UserPrincipalTest {

    @Test
    void exposeLeLoginCommeUsername() {
        User user = UserTestBuilder.aUser().build();
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getUsername()).isEqualTo("alice");
    }

    @Test
    void exposeLeHashCommePassword() {
        User user = UserTestBuilder.aUser().build();
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getPassword()).isEqualTo("hash");
    }

    @Test
    void prefixeLeRoleParRolePourSpringSecurity() {
        User admin = UserTestBuilder.aUser()
                .withLogin("admin")
                .withEmail("admin@homerunner.local")
                .withRole(UserRole.ADMIN)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void donneAccesAuUserSousJacent() {
        User user = UserTestBuilder.aUser().build();
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getUser()).isSameAs(user);
    }
}
