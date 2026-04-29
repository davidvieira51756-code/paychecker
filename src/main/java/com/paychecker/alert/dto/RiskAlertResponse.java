package com.paychecker.alert.dto;

import com.paychecker.alert.domain.RiskAlertSeverity;
import com.paychecker.alert.domain.RiskAlertStatus;

import java.time.Instant;

public record RiskAlertResponse(
        Long id,
        Long paymentId,
        Long accountId,
        Integer riskScore,
        RiskAlertSeverity severity,
        RiskAlertStatus status,
        String reasonSummary,
        Instant createdAt,
        Instant updatedAt
) {
}