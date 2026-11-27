package com.merchantassistant.purchase;

import com.merchantassistant.product.Product;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private InventoryAdjustmentReason reasonType;

    @Column(nullable = false)
    private Integer beforeStock;

    @Column(nullable = false)
    private Integer actualStock;

    @Column(nullable = false)
    private Integer quantityDifference;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public InventoryAdjustmentReason getReasonType() { return reasonType; }
    public void setReasonType(InventoryAdjustmentReason reasonType) { this.reasonType = reasonType; }
    public Integer getBeforeStock() { return beforeStock; }
    public void setBeforeStock(Integer beforeStock) { this.beforeStock = beforeStock; }
    public Integer getActualStock() { return actualStock; }
    public void setActualStock(Integer actualStock) { this.actualStock = actualStock; }
    public Integer getQuantityDifference() { return quantityDifference; }
    public void setQuantityDifference(Integer quantityDifference) { this.quantityDifference = quantityDifference; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
}
