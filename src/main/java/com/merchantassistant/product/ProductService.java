package com.merchantassistant.product;

import com.merchantassistant.common.ProductNotFoundException;
import com.merchantassistant.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.merchantassistant.store.StoreContext;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductCategoryService categoryService;

    public ProductService(ProductRepository repository, ProductCategoryService categoryService) {
        this.repository = repository;
        this.categoryService = categoryService;
    }

    public List<Product> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findByStoreIdOrderByUpdatedAtDesc(StoreContext.get());
        }
        long storeId = StoreContext.get();
        return repository.findByStoreIdAndNameContainingIgnoreCaseOrStoreIdAndSkuContainingIgnoreCase(storeId, keyword.trim(), storeId, keyword.trim());
    }

    public PageResponse<Product> page(int page, int size, String keyword, String category, Boolean enabled, boolean lowStock) {
        Specification<Product> specification = (root, query, builder) -> builder.equal(root.get("storeId"), StoreContext.get());
        if (keyword != null && !keyword.isBlank()) {
            String value = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), value),
                    builder.like(builder.lower(root.get("sku")), value)));
        }
        if (category != null && !category.isBlank()) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("category"), category.trim()));
        }
        if (enabled != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("enabled"), enabled));
        }
        if (lowStock) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("stock"), root.get("stockWarningLine")));
        }
        Page<Product> result = repository.findAll(specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.from(result);
    }

    public Product get(Long id) {
        return repository.findById(id).filter(product -> StoreContext.get() == product.getStoreId()).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        repository.findByStoreIdAndSku(StoreContext.get(), request.sku()).ifPresent(existing -> {
            throw new IllegalArgumentException("商品编码已存在: " + request.sku());
        });
        Product product = new Product();
        product.setStoreId(StoreContext.get());
        copy(request, product);
        return repository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = get(id);
        repository.findByStoreIdAndSku(StoreContext.get(), request.sku()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("商品编码已存在: " + request.sku());
            }
        });
        copy(request, product);
        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        product.setEnabled(false);
        repository.save(product);
    }

    private void copy(ProductRequest request, Product product) {
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setCategory(categoryService.validateEnabledName(request.category()));
        product.setPurchasePrice(request.purchasePrice());
        product.setSellingPrice(request.sellingPrice());
        product.setStockWarningLine(request.stockWarningLine());
        product.setEnabled(request.enabled() == null || request.enabled());
    }
}
