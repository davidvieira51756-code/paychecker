package com.paychecker.alert.controller;

import com.paychecker.alert.dto.RiskAlertResponse;
import com.paychecker.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.paychecker.alert.dto.UpdateRiskAlertStatusRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<RiskAlertResponse> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/open")
    public List<RiskAlertResponse> getOpenAlerts() {
        return alertService.getOpenAlerts();
    }

    @GetMapping("/{id}")
    public RiskAlertResponse getAlertById(@PathVariable Long id) {
        return alertService.getAlertById(id);
    }

    @PatchMapping("/{id}/status")
    public RiskAlertResponse updateAlertStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRiskAlertStatusRequest request
    ) {
        return alertService.updateAlertStatus(id, request);
    }
}