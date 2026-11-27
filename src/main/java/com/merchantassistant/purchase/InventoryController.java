package com.merchantassistant.purchase;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.merchantassistant.store.StoreContext;

@RestController
@RequestMapping("/api/inventory-transactions")
public class InventoryController {

    private final InventoryTransactionRepository repository;

    public InventoryController(InventoryTransactionRepository repository) { this.repository = repository; }

    @GetMapping
    @Transactional(readOnly = true)
    public List<InventoryTransactionView> list() {
        return repository.findByStoreIdOrderByCreatedAtDesc(StoreContext.get()).stream().map(transaction -> new InventoryTransactionView(
                transaction.getId(), transaction.getProduct().getId(), transaction.getProduct().getName(),
                transaction.getProduct().getSku(), transaction.getType(), transaction.getQuantity(),
                transaction.getBeforeStock(), transaction.getAfterStock(), transaction.getReferenceType(),
                transaction.getReferenceId(), transaction.getCreatedAt())).toList();
    }
}
