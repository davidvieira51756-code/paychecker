package com.paychecker.risk;

import com.paychecker.account.domain.Account;
import com.paychecker.account.domain.AccountStatus;
import com.paychecker.payment.dto.AuthorizePaymentRequest;
import com.paychecker.payment.repository.PaymentRepository;
import com.paychecker.risk.engine.RiskRuleResult;
import com.paychecker.risk.engine.RiskScoreResult;
import com.paychecker.risk.engine.RiskScoringContext;
import com.paychecker.risk.engine.RiskScoringEngine;
import com.paychecker.risk.rules.HighAmountRiskRule;
import com.paychecker.risk.rules.NewBeneficiaryRiskRule;
import com.paychecker.risk.rules.VelocityRiskRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskScoringRulesTest {

    @Test
    void highAmountRiskRuleShouldReturnEmptyForSmallAmount() {
        HighAmountRiskRule rule = new HighAmountRiskRule();

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                requestWithAmount("100.00")
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void highAmountRiskRuleShouldReturnHighAmountForMediumThreshold() {
        HighAmountRiskRule rule = new HighAmountRiskRule();

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                requestWithAmount("2500.00")
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().reason()).isEqualTo("HIGH_AMOUNT");
        assertThat(result.get().scoreImpact()).isEqualTo(30);
    }

    @Test
    void highAmountRiskRuleShouldReturnVeryHighAmountForHighThreshold() {
        HighAmountRiskRule rule = new HighAmountRiskRule();

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                requestWithAmount("6000.00")
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().reason()).isEqualTo("VERY_HIGH_AMOUNT");
        assertThat(result.get().scoreImpact()).isEqualTo(50);
    }

    @Test
    void newBeneficiaryRiskRuleShouldReturnReasonWhenBeneficiaryWasNeverUsed() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(paymentRepository.existsByAccount_IdAndBeneficiaryIban(
                1L,
                "PT50000200000000000000002"
        )).thenReturn(false);

        NewBeneficiaryRiskRule rule = new NewBeneficiaryRiskRule(paymentRepository);

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                validRequest()
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().reason()).isEqualTo("NEW_BENEFICIARY");
        assertThat(result.get().scoreImpact()).isEqualTo(25);
    }

    @Test
    void newBeneficiaryRiskRuleShouldReturnEmptyWhenBeneficiaryWasAlreadyUsed() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(paymentRepository.existsByAccount_IdAndBeneficiaryIban(
                1L,
                "PT50000200000000000000002"
        )).thenReturn(true);

        NewBeneficiaryRiskRule rule = new NewBeneficiaryRiskRule(paymentRepository);

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                validRequest()
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void velocityRiskRuleShouldReturnEmptyWhenRecentPaymentsAreBelowThreshold() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(paymentRepository.countByAccount_IdAndCreatedAtAfter(
                any(Long.class),
                any(Instant.class)
        )).thenReturn(3L);

        VelocityRiskRule rule = new VelocityRiskRule(paymentRepository);

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                validRequest()
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void velocityRiskRuleShouldReturnReasonWhenRecentPaymentsReachThreshold() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(paymentRepository.countByAccount_IdAndCreatedAtAfter(
                any(Long.class),
                any(Instant.class)
        )).thenReturn(5L);

        VelocityRiskRule rule = new VelocityRiskRule(paymentRepository);

        RiskScoringContext context = new RiskScoringContext(
                activeAccount(),
                validRequest()
        );

        Optional<RiskRuleResult> result = rule.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().reason()).isEqualTo("HIGH_PAYMENT_VELOCITY");
        assertThat(result.get().scoreImpact()).isEqualTo(40);
    }

    @Test
    void riskScoringEngineShouldSumRuleScores() {
        RiskScoringEngine engine = new RiskScoringEngine(List.of(
                context -> Optional.of(new RiskRuleResult("RULE_A", 30)),
                context -> Optional.of(new RiskRuleResult("RULE_B", 25)),
                context -> Optional.empty()
        ));

        RiskScoreResult result = engine.evaluate(
                new RiskScoringContext(activeAccount(), validRequest())
        );

        assertThat(result.score()).isEqualTo(55);
        assertThat(result.reasons()).containsExactly("RULE_A", "RULE_B");
    }

    @Test
    void riskScoringEngineShouldCapScoreAt100() {
        RiskScoringEngine engine = new RiskScoringEngine(List.of(
                context -> Optional.of(new RiskRuleResult("RULE_A", 70)),
                context -> Optional.of(new RiskRuleResult("RULE_B", 50))
        ));

        RiskScoreResult result = engine.evaluate(
                new RiskScoringContext(activeAccount(), validRequest())
        );

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reasons()).containsExactly("RULE_A", "RULE_B");
    }

    private Account activeAccount() {
        return Account.builder()
                .id(1L)
                .ownerName("David Vieira")
                .iban("PT50000100000000000000001")
                .currency("EUR")
                .balance(new BigDecimal("10000.00"))
                .dailyLimit(new BigDecimal("10000.00"))
                .monthlyLimit(new BigDecimal("50000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    private AuthorizePaymentRequest validRequest() {
        return requestWithAmount("250.00");
    }

    private AuthorizePaymentRequest requestWithAmount(String amount) {
        return new AuthorizePaymentRequest(
                1L,
                new BigDecimal(amount),
                "EUR",
                "PT50000200000000000000002",
                "Empresa XPTO",
                "PT"
        );
    }
}