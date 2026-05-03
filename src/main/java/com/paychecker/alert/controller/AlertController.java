package com.paychecker.alert.controller;

import com.paychecker.alert.dto.RiskAlertResponse;
import com.paychecker.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.paychecker.alert.dto.UpdateRiskAlertStatusRequest;
import jakarta.validation.Valid;
import com.paychecker.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public PageResponse<RiskAlertResponse> getAllAlerts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return alertService.getAllAlerts(pageable);
    }

    @GetMapping("/open")
    public PageResponse<RiskAlertResponse> getOpenAlerts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return alertService.getOpenAlerts(pageable);
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