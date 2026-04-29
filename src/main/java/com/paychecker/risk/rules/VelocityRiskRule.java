package com.paychecker.risk.rules;

import com.paychecker.payment.repository.PaymentRepository;
import com.paychecker.risk.engine.RiskRuleResult;
import com.paychecker.risk.engine.RiskScoringContext;
import com.paychecker.risk.engine.RiskScoringRule;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@Order(30)
@RequiredArgsConstructor
public class VelocityRiskRule implements RiskScoringRule {

    private static final int MAX_PAYMENTS_IN_WINDOW = 5;
    private static final int WINDOW_MINUTES = 10;

    private final PaymentRepository paymentRepository;

    @Override
    public Optional<RiskRuleResult> evaluate(RiskScoringContext context) {
        Instant windowStart = Instant.now().minus(WINDOW_MINUTES, ChronoUnit.MINUTES);

        long recentPayments = paymentRepository.countByAccount_IdAndCreatedAtAfter(
                context.account().getId(),
                windowStart
        );

        if (recentPayments >= MAX_PAYMENTS_IN_WINDOW) {
            return Optional.of(new RiskRuleResult("HIGH_PAYMENT_VELOCITY", 40));
        }

        return Optional.empty();
    }
}