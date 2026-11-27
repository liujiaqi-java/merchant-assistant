package com.merchantassistant.purchase;

import jakarta.validation.Valid;
import com.merchantassistant.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) { this.service = service; }

    @GetMapping
    public List<PurchaseOrderView> list() { return service.list(); }

    @GetMapping("/page")
    public PageResponse<PurchaseOrderView> page(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) PurchaseOrderStatus status,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                @RequestParam(required = false) String keyword) {
        return service.page(page, size, status, from, to, keyword);
    }

    @GetMapping("/{id}")
    public PurchaseOrderView get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderView create(@Valid @RequestBody PurchaseOrderRequest request) { return service.create(request); }

    @PostMapping("/{id}/receive")
    public PurchaseOrderView receive(@PathVariable Long id) { return service.receive(id); }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderView cancel(@PathVariable Long id) { return service.cancel(id); }
}
