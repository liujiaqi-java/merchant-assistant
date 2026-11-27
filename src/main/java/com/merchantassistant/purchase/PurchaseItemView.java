package com.merchantassistant.purchase;

import java.math.BigDecimal;

public record PurchaseItemView(Long productId, String productName, String sku, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
