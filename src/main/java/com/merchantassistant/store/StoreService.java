package com.merchantassistant.store;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StoreService {
    private final StoreRepository repository;
    public StoreService(StoreRepository repository) { this.repository = repository; }

    @PostConstruct @Transactional
    void seedDefaultStore() {
        if (repository.count() == 0) {
            Store store = new Store(); store.setName("婷婷便利店"); store.setAddress("当前经营门店"); repository.save(store);
        }
    }
    public List<Store> list() { return repository.findByEnabledTrueOrderById(); }
    public Store require(long id) { return repository.findById(id).filter(s -> Boolean.TRUE.equals(s.getEnabled())).orElseThrow(() -> new IllegalArgumentException("门店不存在或已停用")); }
    @Transactional public Store create(StoreRequest request) { Store store = new Store(); store.setName(request.name().trim()); store.setAddress(request.address() == null ? null : request.address().trim()); return repository.save(store); }
}
