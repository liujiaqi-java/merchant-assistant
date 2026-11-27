package com.merchantassistant.transfer;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/data-transfer")
public class DataTransferController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final DataTransferService service;

    public DataTransferController(DataTransferService service) { this.service = service; }

    @GetMapping("/products/template")
    public ResponseEntity<byte[]> productTemplate() {
        return download(service.productTemplate(), "product-import-template.xlsx");
    }

    @PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImportResult importProducts(@RequestParam("file") MultipartFile file) {
        return service.importProducts(file);
    }

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProducts() {
        return download(service.exportProducts(), filename("products"));
    }

    @GetMapping("/purchases/export")
    public ResponseEntity<byte[]> exportPurchases() {
        return download(service.exportPurchases(), filename("purchase-orders"));
    }

    @GetMapping("/sales/export")
    public ResponseEntity<byte[]> exportSales() {
        return download(service.exportSales(), filename("sale-orders"));
    }

    @GetMapping("/inventory/export")
    public ResponseEntity<byte[]> exportInventory() {
        return download(service.exportInventory(), filename("inventory-transactions"));
    }

    private ResponseEntity<byte[]> download(byte[] content, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.length).body(content);
    }

    private String filename(String prefix) { return prefix + "-" + LocalDate.now() + ".xlsx"; }
}
