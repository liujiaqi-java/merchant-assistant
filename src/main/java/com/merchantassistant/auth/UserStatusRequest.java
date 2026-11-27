package com.merchantassistant.auth;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull Boolean enabled) { }
