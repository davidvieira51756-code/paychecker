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
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import com.paychecker.alert.dto.UpdateRiskAlertStatusRequest;
import org.springframework.web.server.ResponseStatusException;
import com.paychecker.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final RiskAlertRepository riskAlertRepository;
    private final EventLogService eventLogService;

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

        eventLogService.recordEvent(
                EventType.RISK_ALERT_CREATED,
                "RISK_ALERT",
                savedAlert.getId(),
                Map.of(
                        "paymentId", savedAlert.getPayment().getId(),
                        "accountId", savedAlert.getAccount().getId(),
                        "riskScore", savedAlert.getRiskScore(),
                        "severity", savedAlert.getSeverity().name(),
                        "status", savedAlert.getStatus().name()
                )
        );

        return toResponse(savedAlert);
    }

    @Transactional(readOnly = true)
    public PageResponse<RiskAlertResponse> getAllAlerts(Pageable pageable) {
        return PageResponse.from(
                riskAlertRepository.findAll(pageable)
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RiskAlertResponse> getOpenAlerts(Pageable pageable) {
        return PageResponse.from(
                riskAlertRepository.findByStatus(RiskAlertStatus.OPEN, pageable)
                        .map(this::toResponse)
        );
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

    @Transactional(readOnly = true)
    public RiskAlertResponse getAlertById(Long id) {
        RiskAlert alert = riskAlertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk alert not found"));

        return toResponse(alert);
    }

    @Transactional
    public RiskAlertResponse updateAlertStatus(Long id, UpdateRiskAlertStatusRequest request) {
        RiskAlert alert = riskAlertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk alert not found"));

        RiskAlertStatus previousStatus = alert.getStatus();

        alert.setStatus(request.status());

        RiskAlert savedAlert = riskAlertRepository.save(alert);

        eventLogService.recordEvent(
                EventType.RISK_ALERT_STATUS_UPDATED,
                "RISK_ALERT",
                savedAlert.getId(),
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "newStatus", savedAlert.getStatus().name(),
                        "paymentId", savedAlert.getPayment().getId(),
                        "accountId", savedAlert.getAccount().getId(),
                        "riskScore", savedAlert.getRiskScore()
                )
        );

        return toResponse(savedAlert);
    }
}