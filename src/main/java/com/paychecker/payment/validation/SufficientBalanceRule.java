package com.paychecker.payment.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(30)
public class SufficientBalanceRule implements PaymentValidationRule {

    @Override
    public Optional<String> validate(PaymentValidationContext context) {
        if (context.account().getBalance().compareTo(context.request().amount()) < 0) {
            return Optional.of("INSUFFICIENT_BALANCE");
        }

        return Optional.empty();
    }
}