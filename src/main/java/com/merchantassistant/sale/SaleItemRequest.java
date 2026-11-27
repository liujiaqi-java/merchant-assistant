package com.merchantassistant.sale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(
        @NotNull(message = "商品不能为空") Long productId,
        @NotNull @Min(value = 1, message = "销售数量必须大于 0") Integer quantity
) {
}
