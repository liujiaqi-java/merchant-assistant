package com.merchantassistant.purchase;

import com.merchantassistant.product.Product;
import com.merchantassistant.product.ProductRepository;
import com.merchantassistant.store.StoreContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryAdjustmentService {
    private final InventoryAdjustmentRepository repository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public InventoryAdjustmentService(InventoryAdjustmentRepository repository,
                                      ProductRepository productRepository,
                                      InventoryService inventoryService) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<InventoryAdjustmentView> list() {
        return repository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get()).stream()
                .map(this::toView).toList();
    }

    @Transactional
    public InventoryAdjustmentView create(InventoryAdjustmentRequest request) {
        Product product = productRepository.findById(request.productId())
                .filter(value -> value.getStoreId().equals(StoreContext.get()))
                .orElseThrow(() -> new IllegalArgumentException("商品不存在或不属于当前门店"));
        int before = product.getStock();
        int difference = request.actualStock() - before;
        if (difference == 0) throw new IllegalArgumentException("实际库存与系统库存相同，无需调整");

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setStoreId(StoreContext.get());
        adjustment.setProduct(product);
        adjustment.setReasonType(request.reasonType());
        adjustment.setBeforeStock(before);
        adjustment.setActualStock(request.actualStock());
        adjustment.setQuantityDifference(difference);
        adjustment.setRemark(request.remark() == null || request.remark().isBlank() ? null : request.remark().trim());
        repository.save(adjustment);

        inventoryService.apply(product, difference, InventoryTransactionType.ADJUSTMENT,
                "INVENTORY_ADJUSTMENT", adjustment.getId());
        return toView(adjustment);
    }

    private InventoryAdjustmentView toView(InventoryAdjustment adjustment) {
        return new InventoryAdjustmentView(adjustment.getId(), adjustment.getProduct().getId(),
                adjustment.getProduct().getName(), adjustment.getProduct().getSku(), adjustment.getReasonType(),
                adjustment.getBeforeStock(), adjustment.getActualStock(), adjustment.getQuantityDifference(),
                adjustment.getRemark(), adjustment.getCreatedAt());
    }
}
