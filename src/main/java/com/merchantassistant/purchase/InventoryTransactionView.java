package com.merchantassistant.purchase;

import java.time.Instant;

public record InventoryTransactionView(Long id, Long productId, String productName, String sku,
                                       InventoryTransactionType type, Integer quantity,
                                       Integer beforeStock, Integer afterStock,
                                       String referenceType, Long referenceId, Instant createdAt) {
}
