package com.merchantassistant.purchase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndReceivePurchaseOrder() throws Exception {
        String supplierBody = objectMapper.writeValueAsString(Map.of(
                "name", "测试供应商", "contact", "张三", "phone", "13800000000"));
        String supplierResponse = mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON).content(supplierBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long supplierId = objectMapper.readTree(supplierResponse).get("id").asLong();

        String productBody = objectMapper.writeValueAsString(Map.of(
                "sku", "PURCHASE-001", "name", "测试商品", "category", "食品",
                "purchasePrice", new BigDecimal("4.00"), "sellingPrice", new BigDecimal("6.00"),
                "stockWarningLine", 3));
        String productResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(productBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(productResponse).get("id").asLong();

        String orderBody = objectMapper.writeValueAsString(Map.of(
                "supplierId", supplierId,
                "items", java.util.List.of(Map.of("productId", productId, "quantity", 8, "unitPrice", new BigDecimal("3.50"))),
                "remark", "测试入库"));
        String orderResponse = mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(28.0))
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(orderResponse).get("id").asLong();

        mockMvc.perform(post("/api/purchase-orders/" + orderId + "/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(8));

        String cancelledOrderResponse = mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long cancelledOrderId = objectMapper.readTree(cancelledOrderResponse).get("id").asLong();
        mockMvc.perform(post("/api/purchase-orders/" + cancelledOrderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
