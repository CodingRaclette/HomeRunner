package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.entries.dto.EventDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    @Test
    void getTypeRenvoieEvent() {
        assertThat(new Event().getType()).isEqualTo(EntryType.EVENT);
    }

    @Test
    void applyDataRemplaceLesChampsSansToucherAuxParticipants() {
        // applyData() ne touche pas aux participants : c'est EntryLifeService qui s'en
        // charge separement via setParticipants(). On verifie ici que applyData() ne les
        // ecrase pas au passage.
        User creator = UserTestBuilder.aUser().build();
        User participant = UserTestBuilder.aUser().withLogin("participant").build();
        EventDTO initial = new EventDTO();
        initial.setName("Repas");
        initial.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        initial.setEndDate(LocalDateTime.of(2026, 9, 1, 22, 0));
        Event event = new Event(initial, creator);
        event.participants.add(participant);

        EventDTO update = new EventDTO();
        update.setName("Repas de famille");
        update.setDate(LocalDateTime.of(2026, 9, 2, 20, 0));
        update.setDescription("Amener un dessert");
        update.setEndDate(LocalDateTime.of(2026, 9, 2, 23, 0));

        event.applyData(update);

        assertThat(event.getName()).isEqualTo("Repas de famille");
        assertThat(event.getDate()).isEqualTo(LocalDateTime.of(2026, 9, 2, 20, 0));
        assertThat(event.getDescription()).isEqualTo("Amener un dessert");
        assertThat(event.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 2, 23, 0));
        assertThat(event.getParticipants()).containsExactly(participant);
    }
}
