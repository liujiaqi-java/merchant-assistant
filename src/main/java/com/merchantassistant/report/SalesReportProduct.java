package com.merchantassistant.report;

import java.math.BigDecimal;

public record SalesReportProduct(Long productId, String productName, String sku,
                                 int quantity, BigDecimal salesAmount) { }
