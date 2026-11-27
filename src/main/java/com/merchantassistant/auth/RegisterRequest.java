package com.merchantassistant.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,32}", message = "用户名需为 3-32 位字母、数字或下划线") String username,
        @NotBlank @Size(min = 6, max = 64, message = "密码长度需为 6-64 位") String password,
        @NotBlank @Size(min = 8, max = 64, message = "找回码长度需为 8-64 位") String recoveryCode) {
}
