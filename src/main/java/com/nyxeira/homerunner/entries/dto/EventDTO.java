package com.nyxeira.homerunner.entries.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EventDTO extends EntryDTO {

    private LocalDateTime endDate;
    private List<Long> participantIds;

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public List<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds) {
        this.participantIds = participantIds;
    }
}
