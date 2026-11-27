package com.merchantassistant.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportResponse(
        String source,
        String statusMessage,
        LocalDate from,
        LocalDate to,
        BigDecimal salesAmount,
        int orderCount,
        int productQuantity,
        BigDecimal grossProfit,
        BigDecimal grossMargin,
        BigDecimal salesChangePercent,
        String summary,
        List<String> highlights,
        List<String> actions,
        Instant generatedAt) {
}
