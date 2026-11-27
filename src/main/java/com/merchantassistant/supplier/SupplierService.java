package com.merchantassistant.supplier;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.merchantassistant.store.StoreContext;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) { this.repository = repository; }

    public List<Supplier> list(String keyword) {
        return keyword == null || keyword.isBlank()
                ? repository.findByStoreIdOrderByUpdatedAtDesc(StoreContext.get())
                : repository.findByStoreIdAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(StoreContext.get(), keyword.trim());
    }

    public Supplier get(Long id) {
        return repository.findById(id).filter(supplier -> StoreContext.get() == supplier.getStoreId()).orElseThrow(() -> new IllegalArgumentException("供应商不存在，id=" + id));
    }

    @Transactional
    public Supplier create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setStoreId(StoreContext.get());
        copy(request, supplier);
        return repository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, SupplierRequest request) {
        Supplier supplier = get(id);
        copy(request, supplier);
        return repository.save(supplier);
    }

    @Transactional
    public void disable(Long id) {
        Supplier supplier = get(id);
        supplier.setEnabled(false);
        repository.save(supplier);
    }

    private void copy(SupplierRequest request, Supplier supplier) {
        supplier.setName(request.name().trim());
        supplier.setContact(request.contact());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setEnabled(request.enabled() == null || request.enabled());
    }
}
