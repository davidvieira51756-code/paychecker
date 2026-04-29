package com.paychecker.eventlog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.domain.FinancialEvent;
import com.paychecker.eventlog.dto.FinancialEventResponse;
import com.paychecker.eventlog.repository.FinancialEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventLogService {

    private final FinancialEventRepository financialEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordEvent(EventType eventType, String entityType, Long entityId, Object payload) {
        String payloadJson = toJson(payload);

        FinancialEvent event = FinancialEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .payloadJson(payloadJson)
                .createdBy("SYSTEM")
                .build();

        financialEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<FinancialEventResponse> getAllEvents() {
        return financialEventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialEventResponse> getEventsForEntity(String entityType, Long entityId) {
        return financialEventRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String toJson(Object payload) {
        try {
            Object safePayload = payload == null ? Map.of() : payload;
            return objectMapper.writeValueAsString(safePayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }

    private FinancialEventResponse toResponse(FinancialEvent event) {
        return new FinancialEventResponse(
                event.getId(),
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getPayloadJson(),
                event.getCreatedBy(),
                event.getCreatedAt()
        );
    }
}