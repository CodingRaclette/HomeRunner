package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void exposeLeLoginCommeUsername() {
        User user = new User("alice", "alice@homerunner.local", "hash", UserRole.MEMBER);
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getUsername()).isEqualTo("alice");
    }

    @Test
    void exposeLeHashCommePassword() {
        User user = new User("alice", "alice@homerunner.local", "hash", UserRole.MEMBER);
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getPassword()).isEqualTo("hash");
    }

    @Test
    void prefixeLeRoleParRolePourSpringSecurity() {
        User admin = new User("admin", "admin@homerunner.local", "hash", UserRole.ADMIN);
        UserPrincipal principal = new UserPrincipal(admin);

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void donneAccesAuUserSousJacent() {
        User user = new User("alice", "alice@homerunner.local", "hash", UserRole.MEMBER);
        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getUser()).isSameAs(user);
    }
}
