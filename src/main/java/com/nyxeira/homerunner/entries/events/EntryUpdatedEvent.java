package com.nyxeira.homerunner.entries.events;

import java.util.List;

public record EntryUpdatedEvent(Long entryId, List<Long> oldParticipantIds, List<Long> newParticipantIds, String actorLogin) {}
