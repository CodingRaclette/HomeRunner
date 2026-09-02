package com.nyxeira.homerunner.entrymodel.model;

import com.nyxeira.homerunner.entries.model.*;
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
 * Volontairement placé dans le package entrymodel.model (et non
 * entrymodel.repositories) : Entry/Event/Task n'exposent pas encore de
 * setters pour name/date/creator/endDate (ces mutations arriveront avec
 * applyData() à l'étape entrylife). En restant dans le même package, le
 * test peut positionner ces champs package-private directement pour
 * construire des fixtures réalistes, sans ajouter de setters prématurés
 * à l'API publique des entités.
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
}
