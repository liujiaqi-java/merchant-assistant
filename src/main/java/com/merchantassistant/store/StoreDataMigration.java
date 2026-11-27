package com.merchantassistant.store;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StoreDataMigration {
    private final JdbcTemplate jdbc;
    public StoreDataMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostConstruct @Transactional
    void assignExistingRows() {
        jdbc.update("UPDATE products SET store_id = ? WHERE store_id IS NULL", StoreContext.DEFAULT_STORE_ID);
        jdbc.update("UPDATE suppliers SET store_id = ? WHERE store_id IS NULL", StoreContext.DEFAULT_STORE_ID);
        jdbc.update("UPDATE purchase_orders SET store_id = ? WHERE store_id IS NULL", StoreContext.DEFAULT_STORE_ID);
        jdbc.update("UPDATE sale_orders SET store_id = ? WHERE store_id IS NULL", StoreContext.DEFAULT_STORE_ID);
        jdbc.update("UPDATE inventory_transactions SET store_id = ? WHERE store_id IS NULL", StoreContext.DEFAULT_STORE_ID);
        jdbc.update("UPDATE inventory_adjustments SET store_id = ? WHERE store_id IS NULL", StoreContext.DEFAULT_STORE_ID);
    }
}
