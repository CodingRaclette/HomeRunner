package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Entry est abstraite : on passe par Event comme instance concrète pour tester
 * le comportement générique qu'elle porte (droits d'édition/suppression, participants
 * mutualisés depuis la fusion assignees/participants). Task hérite exactement du même
 * comportement ; ce qui lui est propre est couvert par TaskTest.
 */
class EntryTest {

    private Event anEvent(User creator) {
        Event event = new Event();
        event.creator = creator;
        return event;
    }

    @Test
    void leCreateurPeutEditerSonEntree() {
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        Entry entry = anEvent(creator);

        assertThat(entry.isEditableBy(creator)).isTrue();
    }

    @Test
    void unAutreMembreNePeutPasEditerLEntree() {
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        User autre = UserTestBuilder.aUser().withLogin("autre").build();
        Entry entry = anEvent(creator);

        assertThat(entry.isEditableBy(autre)).isFalse();
    }

    @Test
    void unAdminNePeutPasEditerUneEntreeDontIlNEstPasLeCreateur() {
        // isEditableBy ne fait pas d'exception pour l'ADMIN, contrairement a isDeletableBy :
        // seul le createur peut modifier.
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        User admin = UserTestBuilder.aUser().withLogin("admin").withRole(UserRole.ADMIN).build();
        Entry entry = anEvent(creator);

        assertThat(entry.isEditableBy(admin)).isFalse();
    }

    @Test
    void leCreateurPeutSupprimerSonEntree() {
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        Entry entry = anEvent(creator);

        assertThat(entry.isDeletableBy(creator)).isTrue();
    }

    @Test
    void unAdminPeutSupprimerUneEntreeDontIlNEstPasLeCreateur() {
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        User admin = UserTestBuilder.aUser().withLogin("admin").withRole(UserRole.ADMIN).build();
        Entry entry = anEvent(creator);

        assertThat(entry.isDeletableBy(admin)).isTrue();
    }

    @Test
    void unMembreNiCreateurNiAdminNePeutPasSupprimer() {
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        User autre = UserTestBuilder.aUser().withLogin("autre").withRole(UserRole.MEMBER).build();
        Entry entry = anEvent(creator);

        assertThat(entry.isDeletableBy(autre)).isFalse();
    }

    @Test
    void getParticipantIdsRefleteLeContenuDeParticipants() {
        User creator = UserTestBuilder.aUser().withLogin("createur").build();
        User alice = mock(User.class);
        when(alice.getId()).thenReturn(1L);
        User bob = mock(User.class);
        when(bob.getId()).thenReturn(2L);
        Entry entry = anEvent(creator);
        entry.participants = Set.of(alice, bob);

        assertThat(entry.getParticipantIds()).containsExactlyInAnyOrder(1L, 2L);
    }
}
