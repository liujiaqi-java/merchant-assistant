package com.merchantassistant.sale;

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
class SaleOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldCompleteSaleAndWriteInventoryTransaction() throws Exception {
        long supplierId = idFrom(postJson("/api/suppliers", Map.of("name", "销售测试供应商")));
        long productId = idFrom(postJson("/api/products", Map.of(
                "sku", "SALE-001", "name", "销售测试商品", "category", "食品",
                "purchasePrice", new BigDecimal("4.00"), "sellingPrice", new BigDecimal("6.00"), "stockWarningLine", 2)));
        long purchaseId = idFrom(postJson("/api/purchase-orders", Map.of(
                "supplierId", supplierId, "items", List.of(Map.of(
                        "productId", productId, "quantity", 10, "unitPrice", new BigDecimal("4.00"))))));
        mockMvc.perform(post("/api/purchase-orders/" + purchaseId + "/receive")).andExpect(status().isOk());

        mockMvc.perform(post("/api/sale-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(Map.of("productId", productId, "quantity", 3)), "remark", "测试销售"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalAmount").value(18.0))
                .andExpect(jsonPath("$.grossProfit").value(6.0));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(7));
        mockMvc.perform(get("/api/inventory-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SALE"))
                .andExpect(jsonPath("$[0].beforeStock").value(10))
                .andExpect(jsonPath("$[0].afterStock").value(7));
    }

    @Test
    void shouldRejectSaleWhenStockIsInsufficient() throws Exception {
        long productId = idFrom(postJson("/api/products", Map.of(
                "sku", "SALE-EMPTY", "name", "无库存商品", "category", "食品",
                "purchasePrice", BigDecimal.ONE, "sellingPrice", BigDecimal.TEN, "stockWarningLine", 2)));

        mockMvc.perform(post("/api/sale-orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(Map.of("productId", productId, "quantity", 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("库存不足")));
    }

    private String postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private long idFrom(String body) throws Exception { return objectMapper.readTree(body).get("id").asLong(); }
}
