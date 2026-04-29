package com.paychecker.eventlog.dto;

import com.paychecker.eventlog.domain.EventType;

import java.time.Instant;

public record FinancialEventResponse(
        Long id,
        EventType eventType,
        String entityType,
        Long entityId,
        String payloadJson,
        String createdBy,
        Instant createdAt
) {
}