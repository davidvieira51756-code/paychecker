package com.paychecker.payment.validation;

import java.util.Optional;

public interface PaymentValidationRule {

    Optional<String> validate(PaymentValidationContext context);
}