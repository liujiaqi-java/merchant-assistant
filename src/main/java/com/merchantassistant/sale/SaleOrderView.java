package com.merchantassistant.sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleOrderView(Long id, String orderNo, SaleOrderStatus status, BigDecimal totalAmount,
                            BigDecimal totalCost, BigDecimal grossProfit, String remark,
                            Instant createdAt, List<SaleOrderItemView> items) {
}
