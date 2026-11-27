package com.merchantassistant.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "system_notifications")
public class SystemNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 32)
    private String targetView;

    @Column(length = 48)
    private String referenceType;

    private Long referenceId;

    @Column(nullable = false)
    private boolean readFlag;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant readAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTargetView() { return targetView; }
    public void setTargetView(String targetView) { this.targetView = targetView; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
