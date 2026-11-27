package com.merchantassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification_paging;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.security.enabled=false"
})
class NotificationAndPagingControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldPersistReadAndNavigateBusinessNotifications() throws Exception {
        long supplierId = idFrom(postJson("/api/suppliers", Map.of("name", "通知供应商")));
        long productId = idFrom(postJson("/api/products", product("NOTICE-001", "通知商品", "饮料", 3)));
        long purchaseId = idFrom(postJson("/api/purchase-orders", Map.of(
                "supplierId", supplierId,
                "items", List.of(Map.of("productId", productId, "quantity", 2, "unitPrice", new BigDecimal("2.00"))))));

        String pendingBody = mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetView").value("purchases"))
                .andReturn().getResponse().getContentAsString();
        long notificationId = objectMapper.readTree(pendingBody).get(0).path("id").asLong();

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive")).andExpect(status().isOk());
        mockMvc.perform(post("/api/sale-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("items", List.of(Map.of("productId", productId, "quantity", 2))))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$[0].targetView").value("products"));

        mockMvc.perform(put("/api/notifications/read-all")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].read").value(everyItem(is(true))));
    }

    @Test
    void shouldFilterAndPageProductsPurchasesAndSales() throws Exception {
        long supplierId = idFrom(postJson("/api/suppliers", Map.of("name", "分页供应商")));
        long colaId = idFrom(postJson("/api/products", product("PAGE-COLA", "分页可乐", "饮料", 5)));
        postJson("/api/products", product("PAGE-BREAD", "分页面包", "食品", 2));

        mockMvc.perform(get("/api/products/page").param("keyword", "可乐").param("category", "饮料").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("PAGE-COLA"));

        long purchaseId = idFrom(postJson("/api/purchase-orders", Map.of(
                "supplierId", supplierId,
                "items", List.of(Map.of("productId", colaId, "quantity", 8, "unitPrice", new BigDecimal("2.00"))))));
        mockMvc.perform(get("/api/purchase-orders/page").param("status", "DRAFT").param("keyword", "分页供应商"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive")).andExpect(status().isOk());

        String saleBody = postJson("/api/sale-orders", Map.of("items", List.of(Map.of("productId", colaId, "quantity", 1))));
        JsonNode sale = objectMapper.readTree(saleBody);
        mockMvc.perform(get("/api/sale-orders/page").param("status", "COMPLETED").param("keyword", sale.path("orderNo").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    private Map<String, Object> product(String sku, String name, String category, int warningLine) {
        return Map.of("sku", sku, "name", name, "category", category,
                "purchasePrice", new BigDecimal("2.00"), "sellingPrice", new BigDecimal("5.00"),
                "stockWarningLine", warningLine);
    }

    private String postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private long idFrom(String body) throws Exception { return objectMapper.readTree(body).path("id").asLong(); }
}
