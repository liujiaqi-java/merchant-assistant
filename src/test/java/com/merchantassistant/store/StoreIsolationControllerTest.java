package com.merchantassistant.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:store_isolation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class StoreIsolationControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldIsolateCompleteBusinessDataBetweenStores() throws Exception {
        long branchId = idFrom(postJson("/api/stores", null, Map.of("name", "测试分店")));

        long mainSupplier = idFrom(postJson("/api/suppliers", 1L, Map.of("name", "主店供应商")));
        long mainProduct = idFrom(postJson("/api/products", 1L, product("ISOLATION-001", "主店商品")));
        long purchaseId = idFrom(postJson("/api/purchase-orders", 1L, Map.of(
                "supplierId", mainSupplier,
                "items", List.of(Map.of("productId", mainProduct, "quantity", 10, "unitPrice", new BigDecimal("4.00"))))));
        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive").header("X-Store-Id", 1))
                .andExpect(status().isOk());
        postJson("/api/sale-orders", 1L, Map.of("items", List.of(Map.of("productId", mainProduct, "quantity", 3))));

        mockMvc.perform(get("/api/products").header("X-Store-Id", 1))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].stock").value(7));
        mockMvc.perform(get("/api/sale-orders").header("X-Store-Id", 1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/inventory-transactions").header("X-Store-Id", 1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/products").header("X-Store-Id", branchId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/suppliers").header("X-Store-Id", branchId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/purchase-orders").header("X-Store-Id", branchId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/sale-orders").header("X-Store-Id", branchId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/inventory-transactions").header("X-Store-Id", branchId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/analytics/overview").header("X-Store-Id", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesAmount").value(0))
                .andExpect(jsonPath("$.totalStock").value(0));

        mockMvc.perform(get("/api/products/" + mainProduct).header("X-Store-Id", branchId))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/sale-orders").header("X-Store-Id", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("items", List.of(Map.of("productId", mainProduct, "quantity", 1))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidStoreHeaders() throws Exception {
        mockMvc.perform(get("/api/products").header("X-Store-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("门店 ID 必须是数字"));
        mockMvc.perform(get("/api/products").header("X-Store-Id", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("门店不存在或已停用"));
    }

    private Map<String, Object> product(String sku, String name) {
        return Map.of("sku", sku, "name", name, "category", "食品",
                "purchasePrice", new BigDecimal("4.00"), "sellingPrice", new BigDecimal("6.00"),
                "stockWarningLine", 2);
    }

    private String postJson(String url, Long storeId, Object body) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (storeId != null) request.header("X-Store-Id", storeId);
        return mockMvc.perform(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long idFrom(String body) throws Exception {
        return objectMapper.readTree(body).get("id").asLong();
    }
}
