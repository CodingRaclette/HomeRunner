package com.nyxeira.homerunner.entries.events;

import java.time.LocalDate;

// date = date de l'occurrence concernée, ou null si l'action porte sur la tâche master.
public record SelfUnassignEvent(Long taskId, LocalDate date, String login) {
}
