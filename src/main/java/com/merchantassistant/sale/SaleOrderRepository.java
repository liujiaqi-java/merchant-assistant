package com.merchantassistant.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long>, JpaSpecificationExecutor<SaleOrder> {
    List<SaleOrder> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<SaleOrder> findByStoreIdAndCreatedAtGreaterThanEqual(Long storeId, Instant from);
    List<SaleOrder> findByStoreIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(Long storeId, Instant from, Instant to);
}
