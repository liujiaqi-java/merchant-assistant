package com.merchantassistant.purchase;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
