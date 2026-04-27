package com.paychecker.account.dto;

import com.paychecker.account.domain.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String ownerName,
        String iban,
        String currency,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}