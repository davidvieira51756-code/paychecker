package com.paychecker.alert.service;

import com.paychecker.account.domain.Account;
import com.paychecker.account.domain.AccountStatus;
import com.paychecker.alert.domain.RiskAlert;
import com.paychecker.alert.domain.RiskAlertSeverity;
import com.paychecker.alert.domain.RiskAlertStatus;
import com.paychecker.alert.dto.RiskAlertResponse;
import com.paychecker.alert.dto.UpdateRiskAlertStatusRequest;
import com.paychecker.alert.repository.RiskAlertRepository;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import com.paychecker.payment.domain.Payment;
import com.paychecker.payment.domain.PaymentStatus;
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
class AlertServiceTest {

    @Mock
    private RiskAlertRepository riskAlertRepository;

    @Mock
    private EventLogService eventLogService;

    @InjectMocks
    private AlertService alertService;

    @Test
    void createAlertForPaymentShouldCreateHighSeverityAlert() {
        Payment payment = paymentWithRiskScore(75);

        when(riskAlertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> savedAlert(invocation.getArgument(0), 1L));

        RiskAlertResponse response = alertService.createAlertForPayment(
                payment,
                List.of("VERY_HIGH_AMOUNT", "NEW_BENEFICIARY")
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.paymentId()).isEqualTo(10L);
        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.riskScore()).isEqualTo(75);
        assertThat(response.severity()).isEqualTo(RiskAlertSeverity.HIGH);
        assertThat(response.status()).isEqualTo(RiskAlertStatus.OPEN);
        assertThat(response.reasonSummary()).isEqualTo("VERY_HIGH_AMOUNT, NEW_BENEFICIARY");

        verify(eventLogService).recordEvent(
                eq(EventType.RISK_ALERT_CREATED),
                eq("RISK_ALERT"),
                eq(1L),
                any()
        );
    }

    @Test
    void createAlertForPaymentShouldCreateCriticalSeverityAlert() {
        Payment payment = paymentWithRiskScore(95);

        when(riskAlertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> savedAlert(invocation.getArgument(0), 2L));

        RiskAlertResponse response = alertService.createAlertForPayment(
                payment,
                List.of("CRITICAL_RISK")
        );

        assertThat(response.severity()).isEqualTo(RiskAlertSeverity.CRITICAL);
        assertThat(response.status()).isEqualTo(RiskAlertStatus.OPEN);
    }

    @Test
    void createAlertForPaymentShouldCreateMediumSeverityAlert() {
        Payment payment = paymentWithRiskScore(65);

        when(riskAlertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> savedAlert(invocation.getArgument(0), 3L));

        RiskAlertResponse response = alertService.createAlertForPayment(
                payment,
                List.of("MEDIUM_RISK")
        );

        assertThat(response.severity()).isEqualTo(RiskAlertSeverity.MEDIUM);
    }

    @Test
    void updateAlertStatusShouldUpdateStatusAndRecordEvent() {
        RiskAlert alert = existingAlert(RiskAlertStatus.OPEN);

        when(riskAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(riskAlertRepository.save(any(RiskAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateRiskAlertStatusRequest request =
                new UpdateRiskAlertStatusRequest(RiskAlertStatus.IN_REVIEW);

        RiskAlertResponse response = alertService.updateAlertStatus(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(RiskAlertStatus.IN_REVIEW);

        verify(eventLogService).recordEvent(
                eq(EventType.RISK_ALERT_STATUS_UPDATED),
                eq("RISK_ALERT"),
                eq(1L),
                any()
        );
    }

    @Test
    void updateAlertStatusShouldThrowNotFoundWhenAlertDoesNotExist() {
        when(riskAlertRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateRiskAlertStatusRequest request =
                new UpdateRiskAlertStatusRequest(RiskAlertStatus.CLOSED);

        assertThatThrownBy(() -> alertService.updateAlertStatus(999L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Risk alert not found");

        verify(riskAlertRepository, never()).save(any(RiskAlert.class));
        verify(eventLogService, never()).recordEvent(any(), anyString(), anyLong(), any());
    }

    private RiskAlert savedAlert(RiskAlert alert, Long id) {
        Instant now = Instant.parse("2026-05-03T12:00:00Z");

        alert.setId(id);
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);

        return alert;
    }

    private RiskAlert existingAlert(RiskAlertStatus status) {
        Instant now = Instant.parse("2026-05-03T12:00:00Z");

        return RiskAlert.builder()
                .id(1L)
                .payment(paymentWithRiskScore(75))
                .account(account())
                .riskScore(75)
                .severity(RiskAlertSeverity.HIGH)
                .status(status)
                .reasonSummary("VERY_HIGH_AMOUNT, NEW_BENEFICIARY")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Payment paymentWithRiskScore(int riskScore) {
        Instant now = Instant.parse("2026-05-03T12:00:00Z");

        return Payment.builder()
                .id(10L)
                .account(account())
                .amount(new BigDecimal("6000.00"))
                .currency("EUR")
                .beneficiaryIban("PT50008800000000000000088")
                .beneficiaryName("New Large Beneficiary")
                .beneficiaryCountry("PT")
                .status(PaymentStatus.MANUAL_REVIEW)
                .decisionReason("VERY_HIGH_AMOUNT, NEW_BENEFICIARY")
                .riskScore(riskScore)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Account account() {
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
}