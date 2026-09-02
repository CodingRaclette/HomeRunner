package com.nyxeira.homerunner.common.events;

import java.util.List;

public record EntryUpdatedEvent(Long entryId, List<Long> oldParticipantIds, List<Long> newParticipantIds, String actorLogin) {}
