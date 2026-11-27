package com.merchantassistant.sale;

import jakarta.validation.Valid;
import com.merchantassistant.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/sale-orders")
public class SaleOrderController {

    private final SaleOrderService service;

    public SaleOrderController(SaleOrderService service) { this.service = service; }

    @GetMapping
    public List<SaleOrderView> list() { return service.list(); }

    @GetMapping("/page")
    public PageResponse<SaleOrderView> page(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) SaleOrderStatus status,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(required = false) String keyword) {
        return service.page(page, size, status, from, to, keyword);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleOrderView create(@Valid @RequestBody SaleOrderRequest request) { return service.create(request); }
}
