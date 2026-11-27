package com.merchantassistant.product;

import com.merchantassistant.store.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCategoryService {
    private static final List<String> DEFAULT_CATEGORIES = List.of("饮料", "食品", "日用品", "其他");

    private final ProductCategoryRepository repository;

    public ProductCategoryService(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<ProductCategory> list() {
        ensureDefaults(StoreContext.get());
        return repository.findByStoreIdOrderByEnabledDescNameAsc(StoreContext.get());
    }

    @Transactional
    public ProductCategory create(ProductCategoryRequest request) {
        String name = normalize(request.name());
        repository.findByStoreIdAndNameIgnoreCase(StoreContext.get(), name).ifPresent(existing -> {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        });
        ProductCategory category = new ProductCategory();
        category.setStoreId(StoreContext.get());
        category.setName(name);
        category.setEnabled(request.enabled() == null || request.enabled());
        return repository.save(category);
    }

    @Transactional
    public ProductCategory update(Long id, ProductCategoryRequest request) {
        ProductCategory category = require(id);
        String name = normalize(request.name());
        repository.findByStoreIdAndNameIgnoreCase(StoreContext.get(), name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) throw new IllegalArgumentException("分类名称已存在: " + name);
        });
        category.setName(name);
        category.setEnabled(request.enabled() == null || request.enabled());
        return repository.save(category);
    }

    @Transactional
    public void disable(Long id) {
        ProductCategory category = require(id);
        category.setEnabled(false);
        repository.save(category);
    }

    @Transactional
    public String validateEnabledName(String name) {
        if (name == null || name.isBlank()) return null;
        ensureDefaults(StoreContext.get());
        String normalized = normalize(name);
        ProductCategory category = repository.findByStoreIdAndNameIgnoreCase(StoreContext.get(), normalized)
                .filter(value -> Boolean.TRUE.equals(value.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException("商品分类不存在或已停用: " + normalized));
        return category.getName();
    }

    @Transactional
    public void ensureDefaults(long storeId) {
        for (String name : DEFAULT_CATEGORIES) {
            if (repository.findByStoreIdAndNameIgnoreCase(storeId, name).isEmpty()) {
                ProductCategory category = new ProductCategory();
                category.setStoreId(storeId);
                category.setName(name);
                repository.save(category);
            }
        }
    }

    private ProductCategory require(Long id) {
        return repository.findById(id)
                .filter(category -> category.getStoreId().equals(StoreContext.get()))
                .orElseThrow(() -> new IllegalArgumentException("商品分类不存在"));
    }

    private String normalize(String value) { return value.trim(); }
}
