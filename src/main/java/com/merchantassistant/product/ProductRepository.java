package com.merchantassistant.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByStoreIdAndSku(Long storeId, String sku);
    List<Product> findByStoreIdOrderByUpdatedAtDesc(Long storeId);
    List<Product> findByStoreIdAndNameContainingIgnoreCaseOrStoreIdAndSkuContainingIgnoreCase(Long storeId, String name, Long skuStoreId, String sku);
    List<Product> findByStoreId(Long storeId);
}
