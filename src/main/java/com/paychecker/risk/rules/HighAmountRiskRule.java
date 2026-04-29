package com.paychecker.risk.rules;

import com.paychecker.risk.engine.RiskRuleResult;
import com.paychecker.risk.engine.RiskScoringContext;
import com.paychecker.risk.engine.RiskScoringRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(10)
public class HighAmountRiskRule implements RiskScoringRule {

    private static final BigDecimal MEDIUM_AMOUNT_THRESHOLD = new BigDecimal("2000.00");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000.00");

    @Override
    public Optional<RiskRuleResult> evaluate(RiskScoringContext context) {
        BigDecimal amount = context.request().amount();

        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            return Optional.of(new RiskRuleResult("VERY_HIGH_AMOUNT", 50));
        }

        if (amount.compareTo(MEDIUM_AMOUNT_THRESHOLD) > 0) {
            return Optional.of(new RiskRuleResult("HIGH_AMOUNT", 30));
        }

        return Optional.empty();
    }
}