package com.merchantassistant.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByStoreIdOrderByEnabledDescNameAsc(Long storeId);
    Optional<ProductCategory> findByStoreIdAndNameIgnoreCase(Long storeId, String name);
}
