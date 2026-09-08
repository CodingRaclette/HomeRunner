package com.nyxeira.homerunner.entries.events;

public record TaskValidatedByOtherEvent(Long taskId, boolean state, String actorLogin) {
}
