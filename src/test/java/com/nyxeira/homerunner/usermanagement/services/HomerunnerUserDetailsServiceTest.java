package com.nyxeira.homerunner.usermanagement.services;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import com.nyxeira.homerunner.usermanagement.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

@ExtendWith(MockitoExtension.class)
class HomerunnerUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @Test
    void chargeLUserEtLEnvelopeDansUnUserPrincipal() {
        User user = UserTestBuilder.aUser().build();
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));

        HomerunnerUserDetailsService service = new HomerunnerUserDetailsService(userRepository);
        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) details).getUser()).isSameAs(user);
    }

    @Test
    void leveUsernameNotFoundSiLoginInconnu() {
        when(userRepository.findByLogin("inconnu")).thenReturn(Optional.empty());

        HomerunnerUserDetailsService service = new HomerunnerUserDetailsService(userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("inconnu"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
