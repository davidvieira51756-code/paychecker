package com.paychecker.eventlog.repository;

import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.domain.FinancialEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FinancialEventRepository extends JpaRepository<FinancialEvent, Long> {

    List<FinancialEvent> findByEventType(EventType eventType);

    Page<FinancialEvent> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
}