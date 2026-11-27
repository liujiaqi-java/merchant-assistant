package com.merchantassistant.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:report_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class SalesReportControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void shouldSummarizeSalesAndExportCsv() throws Exception {
        long supplier = id(postJson("/api/suppliers", Map.of("name", "报表供应商")));
        long product = id(postJson("/api/products", Map.of("sku", "REPORT-001", "name", "报表商品", "category", "食品", "purchasePrice", BigDecimal.ONE, "sellingPrice", new BigDecimal("3.00"), "stockWarningLine", 1)));
        long purchase = id(postJson("/api/purchase-orders", Map.of("supplierId", supplier, "items", List.of(Map.of("productId", product, "quantity", 5, "unitPrice", BigDecimal.ONE)))));
        mockMvc.perform(post("/api/purchase-orders/" + purchase + "/receive")).andExpect(status().isOk());
        mockMvc.perform(post("/api/sale-orders").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("items", List.of(Map.of("productId", product, "quantity", 2)))))).andExpect(status().isCreated());
        String day = LocalDate.now().toString();
        mockMvc.perform(get("/api/reports/sales").param("from", day).param("to", day)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(1)).andExpect(jsonPath("$.productQuantity").value(2))
                .andExpect(jsonPath("$.salesAmount").value(6.0)).andExpect(jsonPath("$.grossProfit").value(4.0))
                .andExpect(jsonPath("$.products[0].productName").value("报表商品"));
        mockMvc.perform(get("/api/reports/sales/export").param("from", day).param("to", day)).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("sales-report-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("报表商品")));
    }

    private long id(String response) throws Exception { return mapper.readTree(response).get("id").asLong(); }
    private String postJson(String url, Object body) throws Exception { return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(); }
}
