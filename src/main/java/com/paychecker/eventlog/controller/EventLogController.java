package com.paychecker.eventlog.controller;

import com.paychecker.eventlog.dto.FinancialEventResponse;
import com.paychecker.eventlog.service.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-log")
@RequiredArgsConstructor
public class EventLogController {

    private final EventLogService eventLogService;

    @GetMapping
    public List<FinancialEventResponse> getAllEvents() {
        return eventLogService.getAllEvents();
    }

    @GetMapping("/{entityType}/{entityId}")
    public List<FinancialEventResponse> getEventsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        return eventLogService.getEventsForEntity(entityType, entityId);
    }
}