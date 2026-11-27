package com.merchantassistant.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReplenishmentAdviceResponse(
        String source,
        String statusMessage,
        int coverageDays,
        BigDecimal estimatedCost,
        String summary,
        List<ReplenishmentItemView> items,
        Instant generatedAt) {
}
