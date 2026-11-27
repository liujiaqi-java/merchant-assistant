package com.merchantassistant.purchase;

import com.merchantassistant.supplier.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant receivedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public void addItem(PurchaseOrderItem item) { items.add(item); item.setPurchaseOrder(this); }
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public PurchaseOrderStatus getStatus() { return status; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public List<PurchaseOrderItem> getItems() { return items; }
}
