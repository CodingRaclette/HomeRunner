package com.nyxeira.homerunner.notification.services;

import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.services.EntryLifeService;
import com.nyxeira.homerunner.notification.model.Notification;
import com.nyxeira.homerunner.notification.repositories.NotificationRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test bout-en-bout (vrai contexte Spring, vraie base H2 en mémoire, sans transaction de test
 * qui masquerait le commit) : vérifie que les notifications atteignent réellement la base après
 * un appel de service, pas seulement que l'écouteur est invoqué.
 * <p>
 * Ce test existe car un bug réel n'était détecté par aucun test Mockito (qui appelle les
 * écouteurs directement, hors de tout vrai cycle de transaction) : les écouteurs
 * TransactionalEventListener(AFTER_COMMIT) de NotificationService, sans propagation
 * REQUIRES_NEW, rejoignaient silencieusement la synchronisation finissante de la transaction
 * d'origine au lieu d'en ouvrir une nouvelle — les Notification restaient en attente de flush et
 * n'étaient jamais réellement commitées (cf. NotificationService).
 * <p>
 * Volontairement sans @Transactional sur la classe de test : avec un rollback de test, la
 * transaction ne committe jamais réellement et AFTER_COMMIT ne se déclencherait donc jamais, ce
 * qui masquerait exactement le bug que ce test vise à détecter. En contrepartie, les écritures
 * sont réellement persistées : on isole donc ce test sur sa propre base en mémoire (au lieu du
 * "testdb" partagé par les autres @SpringBootTest/@DataJpaTest) pour ne pas leur laisser de
 * données résiduelles.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:notification_it;DB_CLOSE_DELAY=-1")
class NotificationIntegrationTest {

    @Autowired EntryLifeService entryLifeService;
    @Autowired UserRepository userRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User aUser(String login) {
        return userRepository.save(new User(login, login + "@x.local", login, passwordEncoder.encode("x"), UserRole.MEMBER));
    }

    @Test
    void createEntryPersisteReellementUneNotificationPourLeParticipant() {
        User creator = aUser("createur-create");
        User participant = aUser("participant-create");

        EventDTO dto = new EventDTO();
        dto.setName("Test création");
        dto.setDate(LocalDateTime.now().plusDays(1));
        dto.setParticipantIds(List.of(participant.getId()));
        entryLifeService.createEvent(dto, creator.getLogin());

        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(participant);
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.getFirst().getId()).isNotNull();
    }

    @Test
    void deleteEntryPersisteReellementUneNotificationPourLeParticipant() {
        User creator = aUser("createur-delete");
        User participant = aUser("participant-delete");

        EventDTO dto = new EventDTO();
        dto.setName("Test suppression");
        dto.setDate(LocalDateTime.now().plusDays(1));
        dto.setParticipantIds(List.of(participant.getId()));
        Long id = entryLifeService.createEvent(dto, creator.getLogin());
        notificationRepository.deleteAll(); // isole la notification de suppression de celle de création

        entryLifeService.deleteEntry(id, creator.getLogin());

        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(participant);
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.getFirst().getId()).isNotNull();
        assertThat(notifications.getFirst().getMessage()).contains("Test suppression").contains("supprimée");
    }
}
