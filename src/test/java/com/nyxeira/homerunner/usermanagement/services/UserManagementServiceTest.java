package com.nyxeira.homerunner.usermanagement.services;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import com.nyxeira.homerunner.usermanagement.services.exceptions.InvalidCurrentPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void leveUsernameNotFoundSiLoginInconnu() {
        when(userRepository.findByLogin("inconnu")).thenReturn(Optional.empty());

        UserManagementService service = new UserManagementService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.changePassword("inconnu", "old", "new"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void leveInvalidCurrentPasswordSiLAncienMdpNeCorrespondPas() {
        User user = new User("alice", "alice@homerunner.local", "Alice","hash-actuel", UserRole.MEMBER);
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvais-mdp", "hash-actuel")).thenReturn(false);

        UserManagementService service = new UserManagementService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.changePassword("alice", "mauvais-mdp", "nouveauMdp123"))
                .isInstanceOf(InvalidCurrentPasswordException.class);
    }

    @Test
    void metAJourLeHashEtLeveLeDrapeauMustChangePasswordSiLAncienMdpEstCorrect() {
        User user = new User("alice", "alice@homerunner.local", "Alice","hash-actuel", UserRole.MEMBER);
        user.setMustChangePassword(true);
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bon-mdp", "hash-actuel")).thenReturn(true);
        when(passwordEncoder.encode("nouveauMdp123")).thenReturn("nouveau-hash");

        UserManagementService service = new UserManagementService(userRepository, passwordEncoder);
        User result = service.changePassword("alice", "bon-mdp", "nouveauMdp123");

        assertThat(result).isSameAs(user);
        assertThat(result.getPasswordHash()).isEqualTo("nouveau-hash");
        assertThat(result.isMustChangePassword()).isFalse();
    }
}
