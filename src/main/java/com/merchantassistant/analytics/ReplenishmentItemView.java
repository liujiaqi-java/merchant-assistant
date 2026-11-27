package com.merchantassistant.analytics;

import java.math.BigDecimal;

public record ReplenishmentItemView(
        Long productId,
        String productName,
        String sku,
        int currentStock,
        int warningLine,
        int salesQuantity30Days,
        int recommendedQuantity,
        BigDecimal estimatedCost,
        String reason) {
}
