package com.nyxeira.homerunner.entries.events;

import java.time.LocalDate;

public record OccurrenceCancelledEvent(Long entryId, LocalDate date, String login) {
}
