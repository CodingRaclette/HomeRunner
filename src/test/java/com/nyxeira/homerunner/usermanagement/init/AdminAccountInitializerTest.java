package com.nyxeira.homerunner.usermanagement.init;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test unitaire pur (pas de contexte Spring, pas de base) : UserRepository et
 * PasswordEncoder sont mockés. Ne couvre pas la branche HOMERUNNER_ADMIN_PASSWORD,
 * qui lit System.getenv(...) directement et n'est pas mockable telle quelle.
 */
@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Test
    void neCreeRienSiAdminExiste() throws Exception {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        AdminAccountInitializer initializer = new AdminAccountInitializer(userRepository, passwordEncoder);
        initializer.run(mock(ApplicationArguments.class));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void creeUnAdminAvecMustChangePasswordSiNonExistant() throws Exception {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-fictif");

        AdminAccountInitializer initializer = new AdminAccountInitializer(userRepository, passwordEncoder);
        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User admin = captor.getValue();
        assertThat(admin.getLogin()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.isMustChangePassword()).isTrue();
        assertThat(admin.getPasswordHash()).isEqualTo("hash-fictif");
    }
}
