package com.merchantassistant.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCategoryRequest(
        @NotBlank(message = "分类名称不能为空") @Size(max = 64) String name,
        Boolean enabled
) { }
