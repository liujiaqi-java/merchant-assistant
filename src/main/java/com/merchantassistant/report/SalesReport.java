package com.merchantassistant.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReport(LocalDate from, LocalDate to, long orderCount, int productQuantity,
                          BigDecimal salesAmount, BigDecimal totalCost, BigDecimal grossProfit,
                          BigDecimal grossMargin, List<SalesReportProduct> products) { }
