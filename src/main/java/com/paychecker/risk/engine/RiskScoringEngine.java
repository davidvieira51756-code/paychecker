package com.paychecker.risk.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RiskScoringEngine {

    private final List<RiskScoringRule> rules;

    public RiskScoreResult evaluate(RiskScoringContext context) {
        List<RiskRuleResult> results = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(Optional::stream)
                .toList();

        int score = results.stream()
                .mapToInt(RiskRuleResult::scoreImpact)
                .sum();

        int cappedScore = Math.min(score, 100);

        List<String> reasons = results.stream()
                .map(RiskRuleResult::reason)
                .toList();

        return new RiskScoreResult(cappedScore, reasons);
    }
}