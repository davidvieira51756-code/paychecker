package com.paychecker.eventlog.controller;

import com.paychecker.eventlog.dto.FinancialEventResponse;
import com.paychecker.eventlog.service.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.paychecker.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/api/event-log")
@RequiredArgsConstructor
public class EventLogController {

    private final EventLogService eventLogService;

    @GetMapping
    public PageResponse<FinancialEventResponse> getAllEvents(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return eventLogService.getAllEvents(pageable);
    }

    @GetMapping("/{entityType}/{entityId}")
    public PageResponse<FinancialEventResponse> getEventsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return eventLogService.getEventsForEntity(entityType, entityId, pageable);
    }
}