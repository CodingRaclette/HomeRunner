package com.nyxeira.homerunner.entries.dto;

import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.EntryType;
import com.nyxeira.homerunner.entries.model.Frequency;
import com.nyxeira.homerunner.entries.model.Task;
import com.nyxeira.homerunner.entries.model.occurrences.EventOccurrence;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * fromEntry()/fromEventOccurrence() alimentent le formulaire (form.html) : une regression
 * ici se traduit typiquement par un champ vide/faux a l'edition. Couvre aussi la valeur par
 * defaut de "interval" (cf. le bug de soumission bloquee par un input[min=1] cache avec
 * value=0 quand la case "Recurrent" n'est pas cochee).
 */
class EntryFormDTOTest {

    @Test
    void fromEntrySurUnEventNonRecurrentLaisseIntervalADefautUn() {
        // Le champ "interval" du formulaire porte un min="1" cote HTML ; le laisser a 0
        // (defaut naturel d'un int Java) bloque silencieusement la soumission quand le
        // bloc recurrence est cache.
        Event event = new Event(new EventDTO(), UserTestBuilder.aUser().build());

        EntryFormDTO form = new EntryFormDTO().fromEntry(event);

        assertThat(form.getFrequency()).isNull();
        assertThat(form.getInterval()).isEqualTo(1);
    }

    @Test
    void fromEntrySurUnEventRecurrentReporteFrequenceEtInterval() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion hebdo");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        dto.setFrequency(Frequency.WEEKLY);
        dto.setInterval(3);
        dto.setUntil(LocalDate.of(2026, 12, 31));
        Event event = new Event(dto, UserTestBuilder.aUser().build());

        EntryFormDTO form = new EntryFormDTO().fromEntry(event);

        assertThat(form.getType()).isEqualTo(EntryType.EVENT);
        assertThat(form.getName()).isEqualTo("Reunion hebdo");
        assertThat(form.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 1, 11, 0));
        assertThat(form.getFrequency()).isEqualTo(Frequency.WEEKLY);
        assertThat(form.getInterval()).isEqualTo(3);
        assertThat(form.getUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void fromEntryRameneIntervalAUnSiLaRecurrenceEnPorteUnInvalide() {
        // Meme garde-fou que le defaut "1" du champ interval : une RecurrenceRule
        // persistee avec un interval a 0 (donnee heritee/corrompue) ne doit jamais
        // reapparaitre telle quelle dans le formulaire (cf. le bug du champ HTML min="1").
        EventDTO dto = new EventDTO();
        dto.setFrequency(Frequency.DAILY);
        dto.setInterval(0);
        Event event = new Event(dto, UserTestBuilder.aUser().build());

        EntryFormDTO form = new EntryFormDTO().fromEntry(event);

        assertThat(form.getFrequency()).isEqualTo(Frequency.DAILY);
        assertThat(form.getInterval()).isEqualTo(1);
    }

    @Test
    void fromEntrySurUneTacheReporteLesParticipants() {
        User alice = mock(User.class);
        when(alice.getId()).thenReturn(1L);
        Task task = new Task(new TaskDTO(), UserTestBuilder.aUser().build());
        task.setParticipants(Set.of(alice));

        EntryFormDTO form = new EntryFormDTO().fromEntry(task);

        assertThat(form.getType()).isEqualTo(EntryType.TASK);
        assertThat(form.getParticipantIds()).containsExactly(1L);
        assertThat(form.getInterval()).isEqualTo(1);
    }

    @Test
    void fromEventOccurrenceReporteLesChampsDeLOccurrenceEtLaDateDemandee() {
        EventDTO dto = new EventDTO();
        dto.setName("Reunion");
        dto.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        dto.setEndDate(LocalDateTime.of(2026, 9, 1, 11, 0));
        Event master = new Event(dto, UserTestBuilder.aUser().build());
        EventOccurrence occurrence = new EventOccurrence(master, LocalDate.of(2026, 9, 8));
        EventDTO overrides = new EventDTO();
        overrides.setName("Reunion (visio)");
        overrides.setDescription("Lien envoye par mail");
        occurrence.applyOverrides(overrides);
        LocalDateTime occurrenceDateTime = LocalDateTime.of(2026, 9, 8, 10, 0);

        EntryFormDTO form = new EntryFormDTO().fromEventOccurrence(occurrence, occurrenceDateTime);

        assertThat(form.getType()).isEqualTo(EntryType.EVENT);
        assertThat(form.getName()).isEqualTo("Reunion (visio)");
        assertThat(form.getDescription()).isEqualTo("Lien envoye par mail");
        assertThat(form.getDate()).isEqualTo(occurrenceDateTime);
        // Comportement actuel (documente tel quel, cf. tache de suivi) : pas de surcharge
        // de endDate => EventOccurrence.getEndDate() retombe sur l'endDate ABSOLUE de la
        // master (2026-09-01T11:00), sans la decaler sur la date de cette occurrence
        // (2026-09-08), contrairement a CalendarItemDTO.fromOccurrenceAtDate qui applique
        // ce decalage pour le meme cas.
        assertThat(form.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 1, 11, 0));
    }

    @Test
    void toEventDTOReporteLesChampsDeRecurrenceEtLesParticipants() {
        EntryFormDTO form = new EntryFormDTO();
        form.setName("Anniversaire");
        form.setDate(LocalDateTime.of(2026, 9, 1, 19, 0));
        form.setEndDate(LocalDateTime.of(2026, 9, 1, 23, 0));
        form.setFrequency(Frequency.YEARLY);
        form.setInterval(1);
        form.setParticipantIds(List.of(1L, 2L));

        EventDTO dto = form.toEventDTO();

        assertThat(dto.getName()).isEqualTo("Anniversaire");
        assertThat(dto.getEndDate()).isEqualTo(LocalDateTime.of(2026, 9, 1, 23, 0));
        assertThat(dto.getFrequency()).isEqualTo(Frequency.YEARLY);
        assertThat(dto.getInterval()).isEqualTo(1);
        assertThat(dto.getParticipantIds()).containsExactly(1L, 2L);
    }

    @Test
    void toTaskDTOReporteLesChampsCommunsSansEndDate() {
        EntryFormDTO form = new EntryFormDTO();
        form.setName("Sortir les poubelles");
        form.setDate(LocalDateTime.of(2026, 9, 1, 8, 0));
        form.setParticipantIds(List.of(3L));

        TaskDTO dto = form.toTaskDTO();

        assertThat(dto.getName()).isEqualTo("Sortir les poubelles");
        assertThat(dto.getParticipantIds()).containsExactly(3L);
    }

    @Test
    void isEndDateValidEstVraiPourUneTacheQuelQueSoitEndDate() {
        EntryFormDTO form = new EntryFormDTO();
        form.setType(EntryType.TASK);
        form.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        form.setEndDate(LocalDateTime.of(2026, 9, 1, 9, 0)); // avant la date de debut

        assertThat(form.isEndDateValid()).isTrue();
    }

    @Test
    void isEndDateValidEstFauxPourUnEventDontLaFinPrecedeLeDebut() {
        EntryFormDTO form = new EntryFormDTO();
        form.setType(EntryType.EVENT);
        form.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        form.setEndDate(LocalDateTime.of(2026, 9, 1, 9, 0));

        assertThat(form.isEndDateValid()).isFalse();
    }

    @Test
    void isEndDateValidEstVraiPourUnEventSansDateDeFin() {
        EntryFormDTO form = new EntryFormDTO();
        form.setType(EntryType.EVENT);
        form.setDate(LocalDateTime.of(2026, 9, 1, 10, 0));

        assertThat(form.isEndDateValid()).isTrue();
    }
}
