package com.nyxeira.homerunner.entrylife.dto;


import java.util.List;

public class TaskDTO extends EntryDTO {

    private List<Long> assigneeIds;

    public List<Long> getAssigneeIds() {
        return assigneeIds;
    }

    public void setAssigneeIds(List<Long> assigneeIds) {
        this.assigneeIds = assigneeIds;
    }
}
