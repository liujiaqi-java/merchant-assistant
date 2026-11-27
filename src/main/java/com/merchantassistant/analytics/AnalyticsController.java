package com.merchantassistant.analytics;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) { this.service = service; }

    @GetMapping("/overview")
    public AnalyticsOverview overview() { return service.overview(); }

    @GetMapping("/store-comparison")
    public List<StoreComparisonView> storeComparison() { return service.storeComparison(); }

    @PostMapping("/advice")
    public AdviceResponse advice() { return service.advice(); }

    @PostMapping("/weekly-report")
    public WeeklyReportResponse weeklyReport() { return service.weeklyReport(); }

    @PostMapping("/replenishment")
    public ReplenishmentAdviceResponse replenishment() { return service.replenishmentAdvice(); }
}
