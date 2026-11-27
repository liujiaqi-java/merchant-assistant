package com.merchantassistant.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 64) private String username;
    @Column(nullable = false, length = 16) private String method;
    @Column(nullable = false, length = 255) private String path;
    @Column(nullable = false) private Integer status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
