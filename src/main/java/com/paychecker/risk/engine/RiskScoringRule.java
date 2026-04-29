package com.paychecker.risk.engine;

import java.util.Optional;

public interface RiskScoringRule {

    Optional<RiskRuleResult> evaluate(RiskScoringContext context);
}