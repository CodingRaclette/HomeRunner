package com.nyxeira.homerunner.entries.model.occurrences;

import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AbstractOccurrence est abstraite : testee via EventOccurrence comme instance concrete,
 * dans le meme esprit que EntryTest pour Entry. Le point le plus subtil est la surcharge
 * "paresseuse" des participants (ensureOverride) : tant qu'aucune modification n'a ete
 * faite sur l'occurrence, elle doit refleter en direct les participants de la master.
 */
class AbstractOccurrenceTest {

    private Event aMaster() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        dto.setDescription("Description de la master");
        return new Event(dto, UserTestBuilder.aUser().build());
    }

    @Test
    void getParticipantsRefleteEnDirectCeuxDeLaMasterTantQuAucuneSurchargeNAEuLieu() {
        Event master = aMaster();
        User alice = UserTestBuilder.aUser().withLogin("alice").build();
        master.addParticipant(alice);
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));

        assertThat(occurrence.getParticipants()).containsExactly(alice);

        // la master change APRES construction de l'occurrence : sans surcharge, l'occurrence
        // continue de suivre la master en direct (pas de copie figee a la creation).
        User bob = UserTestBuilder.aUser().withLogin("bob").build();
        master.addParticipant(bob);

        assertThat(occurrence.getParticipants()).containsExactlyInAnyOrder(alice, bob);
    }

    @Test
    void addParticipantSurchargeLOccurrenceSansModifierLaMaster() {
        Event master = aMaster();
        User alice = UserTestBuilder.aUser().withLogin("alice").build();
        master.addParticipant(alice);
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));
        User bob = UserTestBuilder.aUser().withLogin("bob").build();

        occurrence.addParticipant(bob);

        assertThat(occurrence.getParticipants()).containsExactlyInAnyOrder(alice, bob);
        assertThat(master.getParticipants()).containsExactly(alice);
        // la surcharge est maintenant figee : un changement ulterieur de la master n'est plus suivi
        User charlie = UserTestBuilder.aUser().withLogin("charlie").build();
        master.addParticipant(charlie);
        assertThat(occurrence.getParticipants()).containsExactlyInAnyOrder(alice, bob);
    }

    @Test
    void removeParticipantSurchargeLOccurrenceEnPartantDesParticipantsDeLaMaster() {
        Event master = aMaster();
        User alice = UserTestBuilder.aUser().withLogin("alice").build();
        User bob = UserTestBuilder.aUser().withLogin("bob").build();
        master.addParticipant(alice);
        master.addParticipant(bob);
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));

        occurrence.removeParticipant(alice);

        assertThat(occurrence.getParticipants()).containsExactly(bob);
        assertThat(master.getParticipants()).containsExactlyInAnyOrder(alice, bob);
    }

    @Test
    void isParticipantDelegueAGetParticipants() {
        Event master = aMaster();
        User alice = UserTestBuilder.aUser().withLogin("alice").build();
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));

        assertThat(occurrence.isParticipant(alice)).isFalse();

        occurrence.addParticipant(alice);

        assertThat(occurrence.isParticipant(alice)).isTrue();
    }

    @Test
    void getNameEtGetDescriptionRetombentSurLaMasterSansSurcharge() {
        Event master = aMaster();
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));

        assertThat(occurrence.getName()).isEqualTo("Reunion");
        assertThat(occurrence.getDescription()).isEqualTo("Description de la master");
    }

    @Test
    void applyOverridesRemplaceNomEtDescriptionSansAffecterLaMaster() {
        Event master = aMaster();
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));
        EventDTO overrides = new EventDTO();
        overrides.setName("Reunion (visio)");
        overrides.setDescription("Lien envoye par mail");

        occurrence.applyOverrides(overrides);

        assertThat(occurrence.getName()).isEqualTo("Reunion (visio)");
        assertThat(occurrence.getDescription()).isEqualTo("Lien envoye par mail");
        assertThat(master.getName()).isEqualTo("Reunion");
        assertThat(master.getDescription()).isEqualTo("Description de la master");
    }
}
