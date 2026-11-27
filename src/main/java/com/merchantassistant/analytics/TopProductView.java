package com.merchantassistant.analytics;

import java.math.BigDecimal;

public record TopProductView(Long productId, String productName, String sku, int quantity, BigDecimal salesAmount) {
}
