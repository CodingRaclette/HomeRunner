package com.nyxeira.homerunner.usermanagement.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le contrat equals()/hashCode() basé sur le login (clé métier),
 * discuté lors de la conception : indispensable pour un usage fiable de
 * User dans un Set (participants, assignees, contributors...).
 */

class UserTest {

    @Test
    void deuxUsersAvecLeMemeLoginSontEgauxMemeSiLeResteDiffere() {
        User u1 = UserTestBuilder.aUser().withPasswordHash("hash1").build();
        User u2 = UserTestBuilder.aUser()
                .withEmail("autre@mail.local")
                .withName("Bob")
                .withPasswordHash("hash2")
                .withRole(UserRole.ADMIN)
                .build();

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }
}

