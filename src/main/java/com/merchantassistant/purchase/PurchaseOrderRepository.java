package com.merchantassistant.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {
    List<PurchaseOrder> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    java.util.Optional<PurchaseOrder> findByIdAndStoreId(Long id, Long storeId);
}
