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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final PaymentValidationEngine paymentValidationEngine;

    @Transactional
    public PaymentAuthorizationResponse authorizePayment(AuthorizePaymentRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Account not found"));

        PaymentValidationContext context = new PaymentValidationContext(account, request);

        List<String> reasons = paymentValidationEngine.validate(context);

        PaymentStatus decision = reasons.isEmpty()
                ? PaymentStatus.APPROVED
                : PaymentStatus.DECLINED;

        if (reasons.isEmpty()) {
            reasons = List.of("PAYMENT_APPROVED");
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
                .riskScore(0)
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