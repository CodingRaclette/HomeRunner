package com.nyxeira.homerunner.entrylife.dto;


import java.time.LocalDateTime;

public abstract class EntryDTO {

    private String name;
    private LocalDateTime date;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
