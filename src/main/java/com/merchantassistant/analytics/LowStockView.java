package com.merchantassistant.analytics;

public record LowStockView(Long productId, String productName, String sku, int stock, int warningLine) {
}
