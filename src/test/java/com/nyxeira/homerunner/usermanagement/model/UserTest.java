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
        User u1 = new User("alice", "alice@homerunner.local", "hash1", UserRole.MEMBER);
        User u2 = new User("alice", "autre@mail.local", "hash2", UserRole.ADMIN);

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    void deuxUsersAvecUnLoginDifferentNeSontPasEgaux() {
        User u1 = new User("alice", "a@homerunner.local", "hash", UserRole.MEMBER);
        User u2 = new User("bob", "b@homerunner.local", "hash", UserRole.MEMBER);

        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    void unUserNestJamaisEgalANullNiAUnAutreType() {
        User u = new User("alice", "a@homerunner.local", "hash", UserRole.MEMBER);

        assertThat(u).isNotEqualTo(null);
        assertThat(u).isNotEqualTo("alice");
    }

    @Test
    void unUserEstEgalALuiMeme() {
        User u = new User("alice", "a@homerunner.local", "hash", UserRole.MEMBER);

        assertThat(u).isEqualTo(u);
    }
}
