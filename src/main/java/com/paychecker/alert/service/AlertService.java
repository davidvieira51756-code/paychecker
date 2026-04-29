package com.paychecker.alert.service;

import com.paychecker.alert.domain.RiskAlert;
import com.paychecker.alert.domain.RiskAlertSeverity;
import com.paychecker.alert.domain.RiskAlertStatus;
import com.paychecker.alert.dto.RiskAlertResponse;
import com.paychecker.alert.repository.RiskAlertRepository;
import com.paychecker.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final RiskAlertRepository riskAlertRepository;

    @Transactional
    public RiskAlertResponse createAlertForPayment(Payment payment, List<String> reasons) {
        RiskAlert alert = RiskAlert.builder()
                .payment(payment)
                .account(payment.getAccount())
                .riskScore(payment.getRiskScore())
                .severity(calculateSeverity(payment.getRiskScore()))
                .status(RiskAlertStatus.OPEN)
                .reasonSummary(String.join(", ", reasons))
                .build();

        RiskAlert savedAlert = riskAlertRepository.save(alert);

        return toResponse(savedAlert);
    }

    @Transactional(readOnly = true)
    public List<RiskAlertResponse> getAllAlerts() {
        return riskAlertRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RiskAlertResponse> getOpenAlerts() {
        return riskAlertRepository.findByStatus(RiskAlertStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RiskAlertSeverity calculateSeverity(int riskScore) {
        if (riskScore >= 90) {
            return RiskAlertSeverity.CRITICAL;
        }

        if (riskScore >= 75) {
            return RiskAlertSeverity.HIGH;
        }

        if (riskScore >= 60) {
            return RiskAlertSeverity.MEDIUM;
        }

        return RiskAlertSeverity.LOW;
    }

    private RiskAlertResponse toResponse(RiskAlert alert) {
        return new RiskAlertResponse(
                alert.getId(),
                alert.getPayment().getId(),
                alert.getAccount().getId(),
                alert.getRiskScore(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getReasonSummary(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }
}