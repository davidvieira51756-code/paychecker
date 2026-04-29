package com.paychecker.payment.service;

import com.paychecker.account.domain.Account;
import com.paychecker.account.domain.AccountStatus;
import com.paychecker.account.repository.AccountRepository;
import com.paychecker.payment.domain.Payment;
import com.paychecker.payment.domain.PaymentStatus;
import com.paychecker.payment.dto.AuthorizePaymentRequest;
import com.paychecker.payment.dto.PaymentAuthorizationResponse;
import com.paychecker.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public PaymentAuthorizationResponse authorizePayment(AuthorizePaymentRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Account not found"));

        List<String> reasons = new ArrayList<>();
        PaymentStatus decision = PaymentStatus.APPROVED;
        int riskScore = 0;

        if (account.getStatus() != AccountStatus.ACTIVE) {
            decision = PaymentStatus.DECLINED;
            reasons.add("ACCOUNT_NOT_ACTIVE");
        }

        if (!account.getCurrency().equals(request.currency())) {
            decision = PaymentStatus.DECLINED;
            reasons.add("CURRENCY_MISMATCH");
        }

        if (account.getBalance().compareTo(request.amount()) < 0) {
            decision = PaymentStatus.DECLINED;
            reasons.add("INSUFFICIENT_BALANCE");
        }

        if (request.amount().compareTo(account.getDailyLimit()) > 0) {
            decision = PaymentStatus.DECLINED;
            reasons.add("DAILY_LIMIT_EXCEEDED");
        }

        if (reasons.isEmpty()) {
            reasons.add("PAYMENT_APPROVED");
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