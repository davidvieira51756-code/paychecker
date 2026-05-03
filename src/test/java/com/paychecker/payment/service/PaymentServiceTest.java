package com.paychecker.payment.service;

import com.paychecker.account.domain.Account;
import com.paychecker.account.domain.AccountStatus;
import com.paychecker.account.repository.AccountRepository;
import com.paychecker.alert.service.AlertService;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentValidationEngine paymentValidationEngine;

    @Mock
    private RiskScoringEngine riskScoringEngine;

    @Mock
    private AlertService alertService;

    @Mock
    private EventLogService eventLogService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void authorizePaymentShouldThrowNotFoundWhenAccountDoesNotExist() {
        AuthorizePaymentRequest request = validRequest();

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.authorizePayment(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Account not found");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventLogService, never()).recordEvent(any(), anyString(), anyLong(), any());
        verify(alertService, never()).createAlertForPayment(any(), anyList());
    }

    @Test
    void authorizePaymentShouldDeclineWhenValidationFails() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = validRequest();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(paymentValidationEngine.validate(any(PaymentValidationContext.class)))
                .thenReturn(List.of("INSUFFICIENT_BALANCE"));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> savedPayment(invocation.getArgument(0), 10L));

        PaymentAuthorizationResponse response = paymentService.authorizePayment(request);

        assertThat(response.paymentId()).isEqualTo(10L);
        assertThat(response.decision()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(response.riskScore()).isEqualTo(0);
        assertThat(response.reasons()).containsExactly("INSUFFICIENT_BALANCE");

        verify(riskScoringEngine, never()).evaluate(any(RiskScoringContext.class));
        verify(alertService, never()).createAlertForPayment(any(), anyList());

        verify(eventLogService).recordEvent(
                eq(EventType.PAYMENT_REQUESTED),
                eq("PAYMENT"),
                eq(10L),
                any()
        );

        verify(eventLogService).recordEvent(
                eq(EventType.PAYMENT_DECLINED),
                eq("PAYMENT"),
                eq(10L),
                any()
        );
    }

    @Test
    void authorizePaymentShouldApproveWhenValidationPassesAndRiskIsLow() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = validRequest();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(paymentValidationEngine.validate(any(PaymentValidationContext.class)))
                .thenReturn(List.of());
        when(riskScoringEngine.evaluate(any(RiskScoringContext.class)))
                .thenReturn(new RiskScoreResult(25, List.of("NEW_BENEFICIARY")));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> savedPayment(invocation.getArgument(0), 11L));

        PaymentAuthorizationResponse response = paymentService.authorizePayment(request);

        assertThat(response.paymentId()).isEqualTo(11L);
        assertThat(response.decision()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.riskScore()).isEqualTo(25);
        assertThat(response.reasons()).containsExactly("NEW_BENEFICIARY");

        verify(alertService, never()).createAlertForPayment(any(), anyList());

        verify(eventLogService).recordEvent(
                eq(EventType.PAYMENT_REQUESTED),
                eq("PAYMENT"),
                eq(11L),
                any()
        );

        verify(eventLogService).recordEvent(
                eq(EventType.PAYMENT_APPROVED),
                eq("PAYMENT"),
                eq(11L),
                any()
        );
    }

    @Test
    void authorizePaymentShouldSendToManualReviewAndCreateAlertWhenRiskIsHigh() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = highRiskRequest();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(paymentValidationEngine.validate(any(PaymentValidationContext.class)))
                .thenReturn(List.of());
        when(riskScoringEngine.evaluate(any(RiskScoringContext.class)))
                .thenReturn(new RiskScoreResult(75, List.of("VERY_HIGH_AMOUNT", "NEW_BENEFICIARY")));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> savedPayment(invocation.getArgument(0), 12L));

        PaymentAuthorizationResponse response = paymentService.authorizePayment(request);

        assertThat(response.paymentId()).isEqualTo(12L);
        assertThat(response.decision()).isEqualTo(PaymentStatus.MANUAL_REVIEW);
        assertThat(response.riskScore()).isEqualTo(75);
        assertThat(response.reasons()).containsExactly("VERY_HIGH_AMOUNT", "NEW_BENEFICIARY");

        verify(alertService).createAlertForPayment(
                argThat(payment -> payment.getId().equals(12L)
                        && payment.getStatus() == PaymentStatus.MANUAL_REVIEW),
                eq(List.of("VERY_HIGH_AMOUNT", "NEW_BENEFICIARY"))
        );

        verify(eventLogService).recordEvent(
                eq(EventType.PAYMENT_REQUESTED),
                eq("PAYMENT"),
                eq(12L),
                any()
        );

        verify(eventLogService).recordEvent(
                eq(EventType.PAYMENT_SENT_TO_REVIEW),
                eq("PAYMENT"),
                eq(12L),
                any()
        );
    }

    private Payment savedPayment(Payment payment, Long id) {
        Instant now = Instant.parse("2026-05-03T12:00:00Z");

        payment.setId(id);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        return payment;
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
        return new AuthorizePaymentRequest(
                1L,
                new BigDecimal("250.00"),
                "EUR",
                "PT50000200000000000000002",
                "Empresa XPTO",
                "PT"
        );
    }

    private AuthorizePaymentRequest highRiskRequest() {
        return new AuthorizePaymentRequest(
                1L,
                new BigDecimal("6000.00"),
                "EUR",
                "PT50008800000000000000088",
                "New Large Beneficiary",
                "PT"
        );
    }
}