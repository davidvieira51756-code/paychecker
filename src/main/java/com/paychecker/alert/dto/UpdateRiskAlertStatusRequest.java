package com.paychecker.alert.dto;

import com.paychecker.alert.domain.RiskAlertStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRiskAlertStatusRequest(

        @NotNull(message = "Alert status is required")
        RiskAlertStatus status
) {
}