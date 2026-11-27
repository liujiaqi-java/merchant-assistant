package com.merchantassistant.purchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PurchaseItemRequest(
        @NotNull(message = "商品不能为空") Long productId,
        @NotNull @Min(value = 1, message = "采购数量必须大于 0") Integer quantity,
        @NotNull @DecimalMin(value = "0.00", message = "采购价不能为负数") BigDecimal unitPrice
) {
}
