package com.paychecker.payment.service;

import com.paychecker.account.domain.Account;
import com.paychecker.account.repository.AccountRepository;
import com.paychecker.payment.domain.Payment;
import com.paychecker.payment.domain.PaymentStatus;
import com.paychecker.payment.dto.AuthorizePaymentRequest;
import com.paychecker.payment.dto.PaymentAuthorizationResponse;
import com.paychecker.payment.repository.PaymentRepository;
import com.paychecker.payment.validation.PaymentValidationContext;
import com.paychecker.payment.validation.PaymentValidationEngine;
import com.paychecker.risk.engine.RiskScoreResult;
import com.paychecker.risk.engine.RiskScoringContext;
import com.paychecker.risk.engine.RiskScoringEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MANUAL_REVIEW_THRESHOLD = 60;

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final PaymentValidationEngine paymentValidationEngine;
    private final RiskScoringEngine riskScoringEngine;

    @Transactional
    public PaymentAuthorizationResponse authorizePayment(AuthorizePaymentRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Account not found"));

        PaymentValidationContext validationContext = new PaymentValidationContext(account, request);
        List<String> validationReasons = paymentValidationEngine.validate(validationContext);

        PaymentStatus decision;
        int riskScore;
        List<String> reasons;

        if (!validationReasons.isEmpty()) {
            decision = PaymentStatus.DECLINED;
            riskScore = 0;
            reasons = validationReasons;
        } else {
            RiskScoringContext riskContext = new RiskScoringContext(account, request);
            RiskScoreResult riskResult = riskScoringEngine.evaluate(riskContext);

            riskScore = riskResult.score();

            if (riskScore >= MANUAL_REVIEW_THRESHOLD) {
                decision = PaymentStatus.MANUAL_REVIEW;
                reasons = riskResult.reasons();
            } else {
                decision = PaymentStatus.APPROVED;
                reasons = riskResult.reasons().isEmpty()
                        ? List.of("PAYMENT_APPROVED")
                        : riskResult.reasons();
            }
        }

        Payment payment = Payment.builder()
                .account(account)
                .amount(request.amount())
                .currency(request.currency())
                .beneficiaryIban(request.beneficiaryIban())
                .beneficiaryName(request.beneficiaryName())
                .beneficiaryCountry(request.beneficiaryCountry())
                .status(decision)
                .decisionReason(String.join(", ", reasons))
                .riskScore(riskScore)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentAuthorizationResponse(
                savedPayment.getId(),
                savedPayment.getStatus(),
                savedPayment.getRiskScore(),
                reasons,
                savedPayment.getCreatedAt()
        );
    }
}