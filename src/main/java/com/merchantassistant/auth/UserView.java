package com.merchantassistant.auth;

public record UserView(Long id, String username, UserRole role, Boolean enabled) {
    static UserView from(SystemUser user) { return new UserView(user.getId(), user.getUsername(), user.getRole(), user.getEnabled()); }
}
