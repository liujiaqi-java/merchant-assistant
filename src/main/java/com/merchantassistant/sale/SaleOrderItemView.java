package com.merchantassistant.sale;

import java.math.BigDecimal;

public record SaleOrderItemView(Long productId, String productName, String sku, Integer quantity,
                                BigDecimal unitPrice, BigDecimal subtotal) {
}
