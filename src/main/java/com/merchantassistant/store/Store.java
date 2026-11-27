package com.merchantassistant.store;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stores")
public class Store {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 64) private String name;
    @Column(length = 128) private String address;
    @Column(nullable = false) private Boolean enabled = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
}
