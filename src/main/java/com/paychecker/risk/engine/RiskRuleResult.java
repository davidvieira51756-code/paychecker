package com.paychecker.risk.engine;

public record RiskRuleResult(
        String reason,
        int scoreImpact
) {
}