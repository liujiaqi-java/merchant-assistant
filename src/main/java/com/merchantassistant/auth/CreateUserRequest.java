package com.merchantassistant.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank @Size(max = 64) String username,
                                @NotBlank @Size(min = 6, max = 64) String password,
                                @NotNull UserRole role) { }
