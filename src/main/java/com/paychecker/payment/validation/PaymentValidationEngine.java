package com.paychecker.payment.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentValidationEngine {

    private final List<PaymentValidationRule> rules;

    public List<String> validate(PaymentValidationContext context) {
        return rules.stream()
                .map(rule -> rule.validate(context))
                .flatMap(Optional::stream)
                .toList();
    }
}