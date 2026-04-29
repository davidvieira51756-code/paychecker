package com.paychecker.alert.controller;

import com.paychecker.alert.dto.RiskAlertResponse;
import com.paychecker.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}