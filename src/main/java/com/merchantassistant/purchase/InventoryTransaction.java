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
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InventoryTransactionType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer beforeStock;

    @Column(nullable = false)
    private Integer afterStock;

    @Column(nullable = false, length = 32)
    private String referenceType;

    @Column(nullable = false)
    private Long referenceId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Product getProduct() { return product; }
    public InventoryTransactionType getType() { return type; }
    public Integer getQuantity() { return quantity; }
    public Integer getBeforeStock() { return beforeStock; }
    public Integer getAfterStock() { return afterStock; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setProduct(Product product) { this.product = product; }
    public void setType(InventoryTransactionType type) { this.type = type; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setBeforeStock(Integer beforeStock) { this.beforeStock = beforeStock; }
    public void setAfterStock(Integer afterStock) { this.afterStock = afterStock; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
}
