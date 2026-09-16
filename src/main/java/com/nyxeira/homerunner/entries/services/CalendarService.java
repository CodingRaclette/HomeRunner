package com.nyxeira.homerunner.entries.services;

import com.nyxeira.homerunner.entries.dto.CalendarItemDTO;
import com.nyxeira.homerunner.entries.model.Entry;
import com.nyxeira.homerunner.entries.model.Event;
import com.nyxeira.homerunner.entries.model.Task;

import com.nyxeira.homerunner.entries.model.occurrences.Occurrence;
import com.nyxeira.homerunner.entries.repositories.EntryRepository;
import com.nyxeira.homerunner.entries.repositories.EventOccurrenceRepository;
import com.nyxeira.homerunner.entries.repositories.TaskOccurrenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class CalendarService {

    private final EntryRepository entryRepository;
    private final EventOccurrenceRepository eventOccurrenceRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;

    public CalendarService(EntryRepository entryRepository, EventOccurrenceRepository eventOccurrenceRepository, TaskOccurrenceRepository taskOccurrenceRepository) {
        this.entryRepository = entryRepository;
        this.eventOccurrenceRepository = eventOccurrenceRepository;
        this.taskOccurrenceRepository = taskOccurrenceRepository;
    }


    public List<CalendarItemDTO> getCalendar(LocalDateTime startDate, LocalDateTime endDate) {
        List<Entry> entries = entryRepository.findInPeriodOrRecurring(startDate, endDate, startDate.toLocalDate());
        List<CalendarItemDTO> calendarItemDTOList = new ArrayList<>();

        List<Long> eventIds = entries.stream().filter(e -> e instanceof Event && e.isRecurring()).map(Entry::getId).toList();
        List<Long> taskIds = entries.stream().filter(e -> e instanceof Task && e.isRecurring()).map(Entry::getId).toList();

        record OccurrenceKey(Long masterId, LocalDate date) {}
        List<Occurrence> materializedOccurrences = new ArrayList<>();

        materializedOccurrences.addAll(eventOccurrenceRepository.findByMasterIdInAndDateBetween(eventIds, startDate.toLocalDate(), endDate.toLocalDate()));
        materializedOccurrences.addAll(taskOccurrenceRepository.findByMasterIdInAndDateBetween(taskIds, startDate.toLocalDate(), endDate.toLocalDate()));

        Map<OccurrenceKey, Occurrence> occurrencesByKey = materializedOccurrences.stream()
                .collect(Collectors.toMap(
                        o -> new OccurrenceKey(o.getMaster().getId(), o.getDate()),
                        Function.identity()
                ));

        for (Entry entry : entries) {
            if (entry.isRecurring()) {
                List<LocalDateTime> calculatedOccurrences = entry.getRecurrence().occurrencesBetween(entry.getDate(), startDate, endDate);
                for (LocalDateTime occurrenceDate : calculatedOccurrences) {
                    Occurrence materializedOccurrence = occurrencesByKey.get(new OccurrenceKey(entry.getId(), occurrenceDate.toLocalDate()));
                    CalendarItemDTO dto = materializedOccurrence != null
                            ? CalendarItemDTO.fromOccurrenceAtDate(materializedOccurrence, occurrenceDate)
                            : CalendarItemDTO.fromEntryAtDate(entry, occurrenceDate, true);
                    calendarItemDTOList.add(dto);
                }
            } else {
                calendarItemDTOList.add(CalendarItemDTO.fromEntryAtDate(entry, entry.getDate(), false));
            }
        }

        return calendarItemDTOList;
    }
}
