package com.merchantassistant.auth;

public record LoginResponse(String token, UserView user) { }
