package com.nyxeira.homerunner.entries.events;

import java.time.LocalDate;

// date = date de l'occurrence concernée, ou null si l'action porte sur la tâche master.
public record ContributionRemovedEvent(Long taskId, LocalDate date, String targetLogin, String ActorLogin) {
}
