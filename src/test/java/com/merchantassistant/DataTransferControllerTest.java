package com.merchantassistant;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:data_transfer;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class DataTransferControllerTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldImportValidRowsReportInvalidRowsAndCreateInventoryTransaction() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", importWorkbook());

        mockMvc.perform(multipart("/api/data-transfer/products/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(3));

        mockMvc.perform(get("/api/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'IMPORT-001')].stock").value(15));
        mockMvc.perform(get("/api/inventory-transactions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referenceType").value("PRODUCT_IMPORT"))
                .andExpect(jsonPath("$[0].afterStock").value(15));
    }

    @Test
    void shouldExportAllSupportedWorkbooks() throws Exception {
        for (String path : new String[]{"products", "purchases", "sales", "inventory"}) {
            byte[] content = mockMvc.perform(get("/api/data-transfer/" + path + "/export"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
                org.junit.jupiter.api.Assertions.assertTrue(workbook.getSheetAt(0).getRow(0).getPhysicalNumberOfCells() > 0);
            }
        }
    }

    private byte[] importWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("商品导入模板");
            String[] headers = {"商品编码", "商品名称", "分类", "采购价", "销售价", "初始库存", "预警库存", "启用状态"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Row valid = sheet.createRow(1);
            Object[] values = {"IMPORT-001", "导入商品", "食品", 2.5, 5.0, 15, 3, "是"};
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Number number) valid.createCell(i).setCellValue(number.doubleValue());
                else valid.createCell(i).setCellValue(values[i].toString());
            }
            Row invalid = sheet.createRow(2);
            invalid.createCell(0).setCellValue("IMPORT-002");
            invalid.createCell(1).setCellValue("错误商品");
            invalid.createCell(2).setCellValue("不存在分类");
            invalid.createCell(3).setCellValue(1);
            invalid.createCell(4).setCellValue(2);
            invalid.createCell(5).setCellValue(1);
            invalid.createCell(6).setCellValue(1);
            invalid.createCell(7).setCellValue("是");
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
