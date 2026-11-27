package com.merchantassistant.sale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_orders")
public class SaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SaleOrderStatus status = SaleOrderStatus.COMPLETED;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "saleOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleOrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public void addItem(SaleOrderItem item) { items.add(item); item.setSaleOrder(this); }
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public SaleOrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public List<SaleOrderItem> getItems() { return items; }
}
