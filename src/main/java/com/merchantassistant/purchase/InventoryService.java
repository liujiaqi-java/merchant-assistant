package com.merchantassistant.purchase;

import com.merchantassistant.product.Product;
import com.merchantassistant.product.ProductRepository;
import com.merchantassistant.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.merchantassistant.store.StoreContext;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public InventoryService(ProductRepository productRepository, InventoryTransactionRepository transactionRepository,
                            NotificationService notificationService) {
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void apply(Product product, int delta, InventoryTransactionType type, String referenceType, Long referenceId) {
        int before = product.getStock();
        int after = before + delta;
        if (after < 0) throw new IllegalArgumentException("库存不足: " + product.getName() + "，当前库存 " + before);
        product.setStock(after);
        productRepository.save(product);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setStoreId(StoreContext.get());
        transaction.setProduct(product);
        transaction.setType(type);
        transaction.setQuantity(Math.abs(delta));
        transaction.setBeforeStock(before);
        transaction.setAfterStock(after);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transactionRepository.save(transaction);
        notificationService.inventoryChanged(product);
    }
}
