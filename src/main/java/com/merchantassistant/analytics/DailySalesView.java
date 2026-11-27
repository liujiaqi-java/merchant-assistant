package com.merchantassistant.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesView(LocalDate date, BigDecimal salesAmount, long orderCount) {
}
