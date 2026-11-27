package com.merchantassistant.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "system_users")
public class SystemUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String username;
    @Column(nullable = false, length = 128) private String passwordHash;
    @Column(length = 128) private String recoveryCodeHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private UserRole role = UserRole.CLERK;
    @Column(nullable = false) private Boolean enabled = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRecoveryCodeHash() { return recoveryCodeHash; }
    public void setRecoveryCodeHash(String recoveryCodeHash) { this.recoveryCodeHash = recoveryCodeHash; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
