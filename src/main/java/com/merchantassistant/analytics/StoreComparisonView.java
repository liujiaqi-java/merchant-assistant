package com.merchantassistant.analytics;

import java.math.BigDecimal;

public record StoreComparisonView(
        Long storeId,
        String storeName,
        String address,
        BigDecimal salesAmount,
        long orderCount,
        BigDecimal grossProfit,
        BigDecimal grossMargin,
        int totalStock,
        int lowStockCount
) { }
