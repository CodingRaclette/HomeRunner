package com.nyxeira.homerunner.entries.events;


public record ContributionAddedEvent(Long taskId, String targetLogin, String ActorLogin) {
}
