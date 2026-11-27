package com.merchantassistant.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaleOrderRequest(
        @NotEmpty(message = "销售单至少需要一件商品") List<@Valid SaleItemRequest> items,
        String remark
) {
}
