package com.paychecker.payment.validation;

import com.paychecker.account.domain.Account;
import com.paychecker.payment.dto.AuthorizePaymentRequest;

public record PaymentValidationContext(
        Account account,
        AuthorizePaymentRequest request
) {
}