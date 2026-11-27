package com.merchantassistant.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String contact;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
