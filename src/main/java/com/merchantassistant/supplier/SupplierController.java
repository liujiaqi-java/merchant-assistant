package com.merchantassistant.supplier;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) { this.service = service; }

    @GetMapping
    public List<Supplier> list(@RequestParam(required = false) String keyword) { return service.list(keyword); }

    @GetMapping("/{id}")
    public Supplier get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Supplier create(@Valid @RequestBody SupplierRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id) { service.disable(id); }
}
