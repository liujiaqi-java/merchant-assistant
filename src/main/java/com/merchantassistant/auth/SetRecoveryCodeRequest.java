package com.merchantassistant.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetRecoveryCodeRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 64, message = "找回码长度需为 8-64 位") String recoveryCode) {
}
