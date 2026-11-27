package com.merchantassistant.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PurchaseOrderRequest(
        @NotNull(message = "供应商不能为空") Long supplierId,
        @NotEmpty(message = "采购单至少需要一件商品") List<@Valid PurchaseItemRequest> items,
        String remark
) {
}
