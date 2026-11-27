package com.merchantassistant.purchase;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-adjustments")
public class InventoryAdjustmentController {
    private final InventoryAdjustmentService service;

    public InventoryAdjustmentController(InventoryAdjustmentService service) { this.service = service; }

    @GetMapping
    public List<InventoryAdjustmentView> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryAdjustmentView create(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return service.create(request);
    }
}
