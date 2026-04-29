package com.paychecker.payment.dto;

import com.paychecker.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.List;

public record PaymentAuthorizationResponse(
        Long paymentId,
        PaymentStatus decision,
        Integer riskScore,
        List<String> reasons,
        Instant createdAt
) {
}