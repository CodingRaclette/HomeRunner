package com.nyxeira.homerunner.entries.events;

import java.time.LocalDate;

public record OccurrenceUpdatedEvent(Long eventId, LocalDate date, String login) {
}
