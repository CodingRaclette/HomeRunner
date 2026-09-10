package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.dto.CalendarItemDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarService {

    private final EntryRepository entryRepository;

    public CalendarService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }


    public List<CalendarItemDTO> getCalendar(LocalDateTime startDate, LocalDateTime endDate) {
        List<Entry> entries = entryRepository.findInPeriodOrRecurring(startDate, endDate, startDate.toLocalDate());
        List<CalendarItemDTO> calendarItemDTOList = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.isRecurring()) {
                List<LocalDateTime> occurrences = entry.getRecurrence().occurrencesBetween(entry.getDate(), startDate, endDate);
                for (LocalDateTime occurrence : occurrences) {
                    calendarItemDTOList.add(CalendarItemDTO.fromEntryAtDate(entry, occurrence));
                }
            } else {
                calendarItemDTOList.add(CalendarItemDTO.fromEntryAtDate(entry, entry.getDate()));
            }
        }
        return calendarItemDTOList;
    }
}
