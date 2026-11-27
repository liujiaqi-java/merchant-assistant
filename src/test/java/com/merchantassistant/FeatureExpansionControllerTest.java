package com.merchantassistant;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:feature_expansion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class FeatureExpansionControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldManageStoreCategoriesAndValidateProductCategory() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        long categoryId = idFrom(postJson("/api/categories", null, Map.of("name", "冷藏食品")));
        postJson("/api/products", null, product("FEATURE-CATEGORY-001", "酸奶", "冷藏食品"));

        mockMvc.perform(delete("/api/categories/" + categoryId)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product("FEATURE-CATEGORY-002", "鲜奶", "冷藏食品"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品分类不存在或已停用: 冷藏食品"));
    }

    @Test
    void shouldAdjustInventoryAndCreateTransaction() throws Exception {
        long supplierId = idFrom(postJson("/api/suppliers", null, Map.of("name", "盘点测试供应商")));
        long productId = idFrom(postJson("/api/products", null, product("FEATURE-STOCK-001", "盘点商品", "食品")));
        long purchaseId = idFrom(postJson("/api/purchase-orders", null, Map.of(
                "supplierId", supplierId,
                "items", List.of(Map.of("productId", productId, "quantity", 10, "unitPrice", new BigDecimal("2.00"))))));
        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive")).andExpect(status().isOk());

        postJson("/api/inventory-adjustments", null, Map.of(
                "productId", productId, "actualStock", 7, "reasonType", "LOSS", "remark", "月末盘点"));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
        mockMvc.perform(get("/api/inventory-adjustments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantityDifference").value(-3))
                .andExpect(jsonPath("$[0].reasonType").value("LOSS"));
        mockMvc.perform(get("/api/inventory-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$[0].afterStock").value(7))
                .andExpect(jsonPath("$[0].referenceType").value("INVENTORY_ADJUSTMENT"));
    }

    @Test
    void shouldCompareBusinessAcrossStores() throws Exception {
        long branchId = idFrom(postJson("/api/stores", null, Map.of("name", "经营对比分店", "address", "城南")));
        long supplierId = idFrom(postJson("/api/suppliers", branchId, Map.of("name", "分店供应商")));
        long productId = idFrom(postJson("/api/products", branchId, product("FEATURE-COMPARE-001", "对比商品", "饮料")));
        long purchaseId = idFrom(postJson("/api/purchase-orders", branchId, Map.of(
                "supplierId", supplierId,
                "items", List.of(Map.of("productId", productId, "quantity", 12, "unitPrice", new BigDecimal("2.00"))))));
        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive").header("X-Store-Id", branchId))
                .andExpect(status().isOk());
        postJson("/api/sale-orders", branchId, Map.of(
                "items", List.of(Map.of("productId", productId, "quantity", 2))));

        String body = mockMvc.perform(get("/api/analytics/store-comparison"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode branch = null;
        for (JsonNode item : objectMapper.readTree(body)) {
            if (item.path("storeId").asLong() == branchId) branch = item;
        }
        org.junit.jupiter.api.Assertions.assertNotNull(branch);
        org.junit.jupiter.api.Assertions.assertEquals(1, branch.path("orderCount").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new BigDecimal("12.00").compareTo(branch.path("salesAmount").decimalValue()));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new BigDecimal("8.00").compareTo(branch.path("grossProfit").decimalValue()));
        org.junit.jupiter.api.Assertions.assertEquals(10, branch.path("totalStock").asInt());
    }

    private Map<String, Object> product(String sku, String name, String category) {
        return Map.of("sku", sku, "name", name, "category", category,
                "purchasePrice", new BigDecimal("2.00"), "sellingPrice", new BigDecimal("6.00"),
                "stockWarningLine", 3);
    }

    private String postJson(String url, Long storeId, Object body) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (storeId != null) request.header("X-Store-Id", storeId);
        return mockMvc.perform(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long idFrom(String body) throws Exception {
        return objectMapper.readTree(body).path("id").asLong();
    }
}
