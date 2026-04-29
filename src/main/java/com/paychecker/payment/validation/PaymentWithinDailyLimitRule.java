package com.paychecker.payment.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(40)
public class PaymentWithinDailyLimitRule implements PaymentValidationRule {

    @Override
    public Optional<String> validate(PaymentValidationContext context) {
        if (context.request().amount().compareTo(context.account().getDailyLimit()) > 0) {
            return Optional.of("DAILY_LIMIT_EXCEEDED");
        }

        return Optional.empty();
    }
}