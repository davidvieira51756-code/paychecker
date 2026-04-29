package com.paychecker.payment.validation;

import com.paychecker.account.domain.AccountStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(10)
public class AccountIsActiveRule implements PaymentValidationRule {

    @Override
    public Optional<String> validate(PaymentValidationContext context) {
        if (context.account().getStatus() != AccountStatus.ACTIVE) {
            return Optional.of("ACCOUNT_NOT_ACTIVE");
        }

        return Optional.empty();
    }
}