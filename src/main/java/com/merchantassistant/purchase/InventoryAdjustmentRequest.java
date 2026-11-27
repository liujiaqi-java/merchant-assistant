package com.merchantassistant.purchase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
        @NotNull Long productId,
        @NotNull @Min(0) Integer actualStock,
        @NotNull InventoryAdjustmentReason reasonType,
        @Size(max = 255) String remark
) { }
