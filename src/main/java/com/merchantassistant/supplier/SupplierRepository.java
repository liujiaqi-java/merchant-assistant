package com.merchantassistant.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByStoreIdOrderByUpdatedAtDesc(Long storeId);
    List<Supplier> findByStoreIdAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(Long storeId, String name);
}
