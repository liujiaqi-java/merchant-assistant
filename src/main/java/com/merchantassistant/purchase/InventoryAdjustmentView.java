package com.merchantassistant.purchase;

import java.time.Instant;

public record InventoryAdjustmentView(
        Long id,
        Long productId,
        String productName,
        String sku,
        InventoryAdjustmentReason reasonType,
        Integer beforeStock,
        Integer actualStock,
        Integer quantityDifference,
        String remark,
        Instant createdAt
) { }
