package com.merchantassistant.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/sales")
public class SalesReportController {
    private final SalesReportService service;
    public SalesReportController(SalesReportService service) { this.service = service; }
    @GetMapping public SalesReport report(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return service.create(from, to); }
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String filename = "sales-report-" + from + "-" + to + ".csv";
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(service.csv(from, to));
    }
}
