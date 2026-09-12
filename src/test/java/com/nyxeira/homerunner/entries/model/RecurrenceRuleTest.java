package com.nyxeira.homerunner.entries.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coeur du calcul de recurrence (sous-ensemble simplifie de la RFC 5545, cf. le
 * commentaire de RecurrenceRule) : entierement pur, sans dependance a la base ou a
 * Spring, donc teste directement sans mock.
 */
class RecurrenceRuleTest {

    @Test
    void genereUneOccurrenceParJourEntreStartEtEndInclus() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 5, 23, 59));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 2, 10, 0),
                LocalDateTime.of(2026, 9, 3, 10, 0),
                LocalDateTime.of(2026, 9, 4, 10, 0),
                LocalDateTime.of(2026, 9, 5, 10, 0)
        );
    }

    @Test
    void nIncluPasLesOccurrencesAvantStartMemeSiLAncreLesPrecede() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 8, 25, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 2, 23, 59));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 2, 10, 0)
        );
    }

    @Test
    void inclutLesBornesStartEtEndExactes() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 2, 10, 0), LocalDateTime.of(2026, 9, 3, 10, 0));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 2, 10, 0),
                LocalDateTime.of(2026, 9, 3, 10, 0)
        );
    }

    @Test
    void sArreteAUntilInclus() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, LocalDate.of(2026, 9, 3));
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 10, 0, 0));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 2, 10, 0),
                LocalDateTime.of(2026, 9, 3, 10, 0)
        );
    }

    @Test
    void neRenvoieRienSiUntilEstAnterieurALAncre() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, LocalDate.of(2026, 8, 1));
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 10, 0, 0));

        assertThat(occurrences).isEmpty();
    }

    @Test
    void excluLesExDates() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, null);
        rule.addExDate(LocalDate.of(2026, 9, 3));
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 5, 23, 59));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 2, 10, 0),
                LocalDateTime.of(2026, 9, 4, 10, 0),
                LocalDateTime.of(2026, 9, 5, 10, 0)
        );
    }

    @Test
    void removeExDateReautoriseUneOccurrencePrecedemmentExclue() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, null);
        LocalDate excluded = LocalDate.of(2026, 9, 3);
        rule.addExDate(excluded);
        rule.removeExDate(excluded);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 3, 23, 59));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 2, 10, 0),
                LocalDateTime.of(2026, 9, 3, 10, 0)
        );
    }

    @Test
    void unIntervalNullNeGenereQueLOccurrenceDeLAncreSansBouclerALInfini() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, null, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59));

        assertThat(occurrences).containsExactly(LocalDateTime.of(2026, 9, 1, 10, 0));
    }

    @Test
    void unIntervalZeroNeGenereQueLOccurrenceDeLAncre() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 0, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59));

        assertThat(occurrences).containsExactly(LocalDateTime.of(2026, 9, 1, 10, 0));
    }

    @Test
    void frequenceHebdomadaireAvanceDIntervalSemaines() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.WEEKLY, 2, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 0, 0));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 29, 10, 0)
        );
    }

    @Test
    void frequenceMensuelleAvanceDIntervalMois() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.MONTHLY, 1, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 15, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 11, 30, 0, 0));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 10, 15, 10, 0),
                LocalDateTime.of(2026, 11, 15, 10, 0)
        );
    }

    @Test
    void frequenceAnnuelleAvanceDIntervalAnnees() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.YEARLY, 1, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 9, 15, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2028, 12, 31, 0, 0));

        assertThat(occurrences).containsExactly(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2027, 9, 15, 10, 0),
                LocalDateTime.of(2028, 9, 15, 10, 0)
        );
    }

    @Test
    void neRenvoieRienSiLAncreEstApresLaFinDeLaPeriode() {
        RecurrenceRule rule = new RecurrenceRule(Frequency.DAILY, 1, null);
        LocalDateTime anchor = LocalDateTime.of(2026, 10, 1, 10, 0);

        List<LocalDateTime> occurrences = rule.occurrencesBetween(anchor,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 0, 0));

        assertThat(occurrences).isEmpty();
    }
}
