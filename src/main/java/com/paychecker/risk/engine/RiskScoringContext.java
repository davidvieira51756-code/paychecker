package com.paychecker.risk.engine;

import com.paychecker.account.domain.Account;
import com.paychecker.payment.dto.AuthorizePaymentRequest;

public record RiskScoringContext(
        Account account,
        AuthorizePaymentRequest request
) {
}