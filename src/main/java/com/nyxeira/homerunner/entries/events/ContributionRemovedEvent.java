package com.nyxeira.homerunner.entries.events;

public record ContributionRemovedEvent(Long taskId, String targetLogin, String ActorLogin) {
}
