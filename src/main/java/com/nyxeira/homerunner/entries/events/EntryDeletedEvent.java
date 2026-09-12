package com.nyxeira.homerunner.entries.events;

import java.util.List;

// entryName et participantIds sont capturés avant le softDelete : une fois l'entrée supprimée,
// le filtre @SQLRestriction sur Entry empêche NotificationService de la recharger (cf. Entry.java).
public record EntryDeletedEvent(Long entryId, String entryName, List<Long> participantIds, String actorLogin) {}
