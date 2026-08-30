package com.nyxeira.homerunner.usermanagement.repositories;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DataJpaTest} : ne démarre que la couche JPA, base H2 en mémoire, rollback
 * automatique après chaque test (pas de pollution entre les méthodes).
 */

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void existsByLoginRepondCorrectement() {
        userRepository.save(new User("alice", "alice@homerunner.local", "hash", UserRole.MEMBER));

        assertThat(userRepository.existsByLogin("alice")).isTrue();
        assertThat(userRepository.existsByLogin("bob")).isFalse();
    }

    @Test
    void existsByEmailRepondCorrectement() {
        userRepository.save(new User("alice", "alice@homerunner.local", "hash", UserRole.MEMBER));

        assertThat(userRepository.existsByEmail("alice@homerunner.local")).isTrue();
        assertThat(userRepository.existsByEmail("inconnu@homerunner.local")).isFalse();
    }

    @Test
    void existsByRoleDistingueAdminEtMember() {
        userRepository.save(new User("admin", "admin@homerunner.local", "hash", UserRole.ADMIN));

        assertThat(userRepository.existsByRole(UserRole.ADMIN)).isTrue();
        assertThat(userRepository.existsByRole(UserRole.MEMBER)).isFalse();
    }

    @Test
    void findByLoginRenvoieLUserOuOptionalVide() {
        userRepository.save(new User("alice", "alice@homerunner.local", "hash", UserRole.MEMBER));

        Optional<User> found = userRepository.findByLogin("alice");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@homerunner.local");

        assertThat(userRepository.findByLogin("bob")).isEmpty();
    }
}
