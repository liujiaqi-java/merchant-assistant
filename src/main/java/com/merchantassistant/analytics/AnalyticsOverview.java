package com.merchantassistant.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsOverview(
        int periodDays,
        BigDecimal salesAmount,
        long orderCount,
        BigDecimal grossProfit,
        BigDecimal grossMargin,
        int totalStock,
        int lowStockCount,
        List<DailySalesView> dailySales,
        List<TopProductView> topProducts,
        List<LowStockView> lowStockProducts
) {
}
