package com.merchantassistant.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverPasswordRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, max = 64, message = "找回码长度需为 8-64 位") String recoveryCode,
        @NotBlank @Size(min = 6, max = 64, message = "新密码长度需为 6-64 位") String newPassword) {
}
