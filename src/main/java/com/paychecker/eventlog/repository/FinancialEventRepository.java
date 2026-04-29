package com.paychecker.eventlog.repository;

import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.domain.FinancialEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialEventRepository extends JpaRepository<FinancialEvent, Long> {

    List<FinancialEvent> findByEventType(EventType eventType);

    List<FinancialEvent> findByEntityTypeAndEntityId(String entityType, Long entityId);
}