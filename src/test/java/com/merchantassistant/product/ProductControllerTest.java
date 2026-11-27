package com.merchantassistant.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndListProduct() throws Exception {
        ProductRequest request = new ProductRequest("SKU-001", "矿泉水", "饮料",
                new BigDecimal("1.50"), new BigDecimal("2.50"), 10, true);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.stock").value(0));

        mockMvc.perform(get("/api/products?keyword=矿泉"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("矿泉水"));
    }

    @Test
    void shouldIgnoreStockFromProductWriteRequests() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "sku", "SKU-STOCK-BOUNDARY", "name", "边界商品", "category", "食品",
                "purchasePrice", new BigDecimal("1.00"), "sellingPrice", new BigDecimal("2.00"),
                "stock", 99, "stockWarningLine", 5, "enabled", true));

        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(java.util.Map.of(
                "sku", "SKU-STOCK-BOUNDARY", "name", "边界商品更新", "category", "食品",
                "purchasePrice", new BigDecimal("1.00"), "sellingPrice", new BigDecimal("2.20"),
                "stock", 88, "stockWarningLine", 5, "enabled", true));
        mockMvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("边界商品更新"))
                .andExpect(jsonPath("$.stock").value(0));
    }
}
