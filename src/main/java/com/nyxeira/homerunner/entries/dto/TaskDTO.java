package com.nyxeira.homerunner.entries.dto;


public class TaskDTO extends EntryDTO {

    private boolean validatedByOther;

    public boolean isValidatedByOther() {
        return validatedByOther;
    }

    public void setValidatedByOther(boolean validatedByOther) {
        this.validatedByOther = validatedByOther;
    }
}