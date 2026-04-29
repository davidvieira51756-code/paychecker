package com.paychecker.risk.rules;

import com.paychecker.payment.repository.PaymentRepository;
import com.paychecker.risk.engine.RiskRuleResult;
import com.paychecker.risk.engine.RiskScoringContext;
import com.paychecker.risk.engine.RiskScoringRule;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(20)
@RequiredArgsConstructor
public class NewBeneficiaryRiskRule implements RiskScoringRule {

    private final PaymentRepository paymentRepository;

    @Override
    public Optional<RiskRuleResult> evaluate(RiskScoringContext context) {
        boolean beneficiaryAlreadyUsed = paymentRepository.existsByAccount_IdAndBeneficiaryIban(
                context.account().getId(),
                context.request().beneficiaryIban()
        );

        if (!beneficiaryAlreadyUsed) {
            return Optional.of(new RiskRuleResult("NEW_BENEFICIARY", 25));
        }

        return Optional.empty();
    }
}