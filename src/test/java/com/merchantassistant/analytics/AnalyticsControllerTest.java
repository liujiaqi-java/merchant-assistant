package com.merchantassistant.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
class AnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldCalculateOverviewAndReturnRuleAdviceWithoutApiKey() throws Exception {
        long supplierId = idFrom(postJson("/api/suppliers", Map.of("name", "分析测试供应商")));
        long productId = idFrom(postJson("/api/products", Map.of(
                "sku", "ANALYTICS-001", "name", "分析测试商品", "category", "饮料",
                "purchasePrice", new BigDecimal("2.00"), "sellingPrice", new BigDecimal("5.00"), "stockWarningLine", 2)));
        long purchaseId = idFrom(postJson("/api/purchase-orders", Map.of(
                "supplierId", supplierId, "items", List.of(Map.of(
                        "productId", productId, "quantity", 5, "unitPrice", new BigDecimal("2.00"))))));
        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive")).andExpect(status().isOk());
        postJson("/api/sale-orders", Map.of("items", List.of(Map.of("productId", productId, "quantity", 4))));

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesAmount").value(20.0))
                .andExpect(jsonPath("$.grossProfit").value(12.0))
                .andExpect(jsonPath("$.grossMargin").value(60.0))
                .andExpect(jsonPath("$.topProducts[0].productName").value("分析测试商品"));

        mockMvc.perform(post("/api/analytics/advice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.actions.length()").value(3));

        mockMvc.perform(post("/api/analytics/weekly-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.salesAmount").value(20.0))
                .andExpect(jsonPath("$.productQuantity").value(4))
                .andExpect(jsonPath("$.highlights.length()").value(3));

        mockMvc.perform(post("/api/analytics/replenishment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.coverageDays").value(14))
                .andExpect(jsonPath("$.items[0].productName").value("分析测试商品"))
                .andExpect(jsonPath("$.items[0].recommendedQuantity").value(3))
                .andExpect(jsonPath("$.estimatedCost").value(6.0));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(1));
        mockMvc.perform(get("/api/inventory-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private String postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private long idFrom(String body) throws Exception { return objectMapper.readTree(body).get("id").asLong(); }
}
