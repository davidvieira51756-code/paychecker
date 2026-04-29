package com.paychecker.payment.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(20)
public class CurrencyMatchesRule implements PaymentValidationRule {

    @Override
    public Optional<String> validate(PaymentValidationContext context) {
        if (!context.account().getCurrency().equals(context.request().currency())) {
            return Optional.of("CURRENCY_MISMATCH");
        }

        return Optional.empty();
    }
}