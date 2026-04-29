package com.paychecker.risk.engine;

import java.util.List;

public record RiskScoreResult(
        int score,
        List<String> reasons
) {
}