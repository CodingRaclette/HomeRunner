package com.nyxeira.homerunner.entries.model;

import com.nyxeira.homerunner.entries.dto.TaskDTO;
import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserTestBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    @Test
    void getTypeRenvoieTask() {
        assertThat(new Task().getType()).isEqualTo(EntryType.TASK);
    }

    @Test
    void nEstPasTermineeParDefaut() {
        assertThat(new Task().isDone()).isFalse();
    }

    @Test
    void devientTermineeDesQuUnContributeurEstAjoute() {
        Task task = new Task();
        User contributor = UserTestBuilder.aUser().build();

        task.addContributor(contributor);

        assertThat(task.isDone()).isTrue();
        assertThat(task.isContributor(contributor)).isTrue();
    }

    @Test
    void redevientAFaireQuandLeSeulContributeurEstRetire() {
        Task task = new Task();
        User contributor = UserTestBuilder.aUser().build();
        task.addContributor(contributor);

        task.removeContributor(contributor);

        assertThat(task.isDone()).isFalse();
        assertThat(task.isContributor(contributor)).isFalse();
    }

    @Test
    void estTermineeSiValidatedByOtherMemeSansContributeur() {
        Task task = new Task();

        task.setValidatedByOther(true);

        assertThat(task.isDone()).isTrue();
        assertThat(task.getContributors()).isEmpty();
    }

    @Test
    void applyDataRemplaceLesChampsSansToucherAuxContributeursNiAValidatedByOther() {
        User creator = UserTestBuilder.aUser().build();
        User contributor = UserTestBuilder.aUser().withLogin("contributeur").build();
        TaskDTO initial = new TaskDTO();
        initial.setName("Sortir les poubelles");
        Task task = new Task(initial, creator);
        task.addContributor(contributor);
        task.setValidatedByOther(true);

        TaskDTO update = new TaskDTO();
        update.setName("Sortir le tri");
        update.setDescription("Bac jaune uniquement");

        task.applyData(update);

        assertThat(task.getName()).isEqualTo("Sortir le tri");
        assertThat(task.getDescription()).isEqualTo("Bac jaune uniquement");
        assertThat(task.isContributor(contributor)).isTrue();
        assertThat(task.isValidatedByOther()).isTrue();
    }
}
