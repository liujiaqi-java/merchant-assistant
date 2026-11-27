package com.merchantassistant.supplier;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "供应商名称不能为空") String name,
        String contact,
        String phone,
        String address,
        Boolean enabled
) {
}
