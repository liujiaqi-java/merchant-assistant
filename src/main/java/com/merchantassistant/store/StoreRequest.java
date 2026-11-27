package com.merchantassistant.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreRequest(@NotBlank @Size(max = 64) String name, @Size(max = 128) String address) { }
