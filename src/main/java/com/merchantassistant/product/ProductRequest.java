package com.merchantassistant.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "商品编码不能为空") String sku,
        @NotBlank(message = "商品名称不能为空") String name,
        String category,
        @NotNull @DecimalMin(value = "0.00") BigDecimal purchasePrice,
        @NotNull @DecimalMin(value = "0.00") BigDecimal sellingPrice,
        @NotNull @Min(0) Integer stockWarningLine,
        Boolean enabled
) {
}
