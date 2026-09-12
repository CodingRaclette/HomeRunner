package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.usermanagement.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;

/**
 * Volontairement placé dans le package entries.model (et non
 * entries.repositories) : Entry/Event/Task n'exposent pas encore de
 * setters pour name/date/creator/endDate/participants (ces mutations
 * arriveront avec applyData()/setParticipants() à l'étape entrylife). En
 * restant dans le même package, le test peut positionner ces champs
 * package-private directement pour construire des fixtures réalistes,
 * sans ajouter de setters prématurés à l'API publique des entités.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EntryRepositoryTest {

    @Autowired
    EntryRepository entryRepository;

    @Autowired
    TestEntityManager em;

    private User persistCreator() {
        User creator = UserTestBuilder.aUser().build();
        return em.persistAndFlush(creator);
    }

    @Test
    void saveAndLoadEventAndTaskWithPolymorphicType() {
        User creator = persistCreator();

        Event event = new Event();
        event.name = "Anniversaire";
        event.date = LocalDateTime.of(2026, 9, 1, 19, 0);
        event.creator = creator;
        event.endDate = LocalDateTime.of(2026, 9, 1, 23, 0);

        Task task = new Task();
        task.name = "Faire les courses";
        task.date = LocalDateTime.of(2026, 9, 2, 10, 0);
        task.creator = creator;

        entryRepository.save(event);
        entryRepository.save(task);
        em.flush();
        em.clear(); // force un vrai rechargement depuis la base, pas le cache 1er niveau

        List<Entry> all = entryRepository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).anySatisfy(e -> {
            assertThat(e).isInstanceOf(Event.class);
            assertThat(e.getType()).isEqualTo(EntryType.EVENT);
            assertThat(e.getName()).isEqualTo("Anniversaire");
        });
        assertThat(all).anySatisfy(e -> {
            assertThat(e).isInstanceOf(Task.class);
            assertThat(e.getType()).isEqualTo(EntryType.TASK);
            assertThat(e.getName()).isEqualTo("Faire les courses");
        });
    }

    @Test
    void uneRecurrenceRuleAvecDesExDatesSurvitAUnCycleSaveReload() {
        User creator = persistCreator();

        Task task = new Task();
        task.name = "Sortir les poubelles";
        task.date = LocalDateTime.of(2026, 9, 1, 8, 0);
        task.creator = creator;

        RecurrenceRule rule = new RecurrenceRule();
        rule.frequency = Frequency.WEEKLY;
        rule.interval = 1;
        rule.exDates = Set.of(LocalDate.of(2026, 9, 8));
        task.recurrence = rule;

        Long id = entryRepository.save(task).getId();
        em.flush();
        em.clear();

        Entry reloaded = entryRepository.findById(id).orElseThrow();

        assertThat(reloaded.getRecurrence()).isNotNull();
        assertThat(reloaded.getRecurrence().getFrequency()).isEqualTo(Frequency.WEEKLY);
        assertThat(reloaded.getRecurrence().getExDates()).containsExactly(LocalDate.of(2026, 9, 8));
    }

    @Test
    void lesParticipantsSontPersistesViaLeChampMutualiseSurEntry() {
        // Depuis la fusion assignees/participants, Event et Task partagent le meme champ
        // "participants" porte par Entry (et donc la meme table de jointure entry_participants).
        // Ce test verifie que ce champ mutualise fonctionne bien de maniere polymorphe pour les
        // deux sous-types, aussi bien a l'ecriture qu'a la relecture.
        User creator = persistCreator();
        User alice = em.persistAndFlush(UserTestBuilder.aUser().withLogin("alice-participant").build());
        User bob = em.persistAndFlush(UserTestBuilder.aUser().withLogin("bob-assignee").build());

        Event event = new Event();
        event.name = "Repas de famille";
        event.date = LocalDateTime.of(2026, 9, 1, 19, 0);
        event.creator = creator;
        event.endDate = LocalDateTime.of(2026, 9, 1, 23, 0);
        event.participants.add(alice);

        Task task = new Task();
        task.name = "Preparer le repas";
        task.date = LocalDateTime.of(2026, 9, 1, 17, 0);
        task.creator = creator;
        task.participants.add(bob);

        entryRepository.save(event);
        entryRepository.save(task);
        em.flush();
        em.clear();

        List<Entry> all = entryRepository.findAll();

        assertThat(all).anySatisfy(e -> {
            assertThat(e).isInstanceOf(Event.class);
            assertThat(e.getParticipantIds()).containsExactly(alice.getId());
        });
        assertThat(all).anySatisfy(e -> {
            assertThat(e).isInstanceOf(Task.class);
            assertThat(e.getParticipantIds()).containsExactly(bob.getId());
        });
    }

    // --- findInPeriodOrRecurring : la requete qui alimente CalendarService.getCalendar ---
    // Une entree "matche" si (a) sa date tombe dans [start, end], OU (b) elle est recurrente
    // et sa recurrence n'est pas deja terminee avant le debut de la periode (until == null,
    // ou until >= periodStart).

    private Task persistTask(User creator, String name, LocalDateTime date) {
        Task task = new Task();
        task.name = name;
        task.date = date;
        task.creator = creator;
        return em.persistAndFlush(task);
    }

    private Task persistRecurringTask(User creator, String name, LocalDateTime date, Frequency frequency, LocalDate until) {
        Task task = new Task();
        task.name = name;
        task.date = date;
        task.creator = creator;
        RecurrenceRule rule = new RecurrenceRule();
        rule.frequency = frequency;
        rule.interval = 1;
        rule.until = until;
        task.recurrence = rule;
        return em.persistAndFlush(task);
    }

    @Test
    void trouveUneEntreeNonRecurrenteDontLaDateTombeDansLaPeriode() {
        User creator = persistCreator();
        persistTask(creator, "Dans la periode", LocalDateTime.of(2026, 9, 15, 10, 0));
        em.clear();

        List<Entry> found = entryRepository.findInPeriodOrRecurring(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59), LocalDate.of(2026, 9, 1));

        assertThat(found).extracting(Entry::getName).containsExactly("Dans la periode");
    }

    @Test
    void nExcluPasUneEntreeNonRecurrenteDontLaDateEstHorsPeriode() {
        User creator = persistCreator();
        persistTask(creator, "Hors periode", LocalDateTime.of(2026, 10, 15, 10, 0));
        em.clear();

        List<Entry> found = entryRepository.findInPeriodOrRecurring(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59), LocalDate.of(2026, 9, 1));

        assertThat(found).isEmpty();
    }

    @Test
    void trouveUneEntreeRecurrenteSansUntilMemeSiSaDateAncreEstAnterieureALaPeriode() {
        User creator = persistCreator();
        persistRecurringTask(creator, "Recurrente sans fin", LocalDateTime.of(2026, 1, 1, 8, 0), Frequency.WEEKLY, null);
        em.clear();

        List<Entry> found = entryRepository.findInPeriodOrRecurring(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59), LocalDate.of(2026, 9, 1));

        assertThat(found).extracting(Entry::getName).containsExactly("Recurrente sans fin");
    }

    @Test
    void trouveUneEntreeRecurrenteDontLUntilEstDansOuApresLaPeriode() {
        User creator = persistCreator();
        persistRecurringTask(creator, "Se termine pendant la periode", LocalDateTime.of(2026, 1, 1, 8, 0),
                Frequency.WEEKLY, LocalDate.of(2026, 9, 15));
        em.clear();

        List<Entry> found = entryRepository.findInPeriodOrRecurring(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59), LocalDate.of(2026, 9, 1));

        assertThat(found).extracting(Entry::getName).containsExactly("Se termine pendant la periode");
    }

    @Test
    void exclutUneEntreeRecurrenteDejaTermineeAvantLeDebutDeLaPeriode() {
        User creator = persistCreator();
        persistRecurringTask(creator, "Deja terminee", LocalDateTime.of(2026, 1, 1, 8, 0),
                Frequency.WEEKLY, LocalDate.of(2026, 8, 1));
        em.clear();

        List<Entry> found = entryRepository.findInPeriodOrRecurring(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59), LocalDate.of(2026, 9, 1));

        assertThat(found).isEmpty();
    }
}
