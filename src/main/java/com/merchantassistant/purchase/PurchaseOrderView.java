package com.merchantassistant.purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderView(
        Long id,
        String orderNo,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        String remark,
        Instant createdAt,
        Instant receivedAt,
        List<PurchaseItemView> items
) {
}
