package com.merchantassistant.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class ProductCategoryController {
    private final ProductCategoryService service;

    public ProductCategoryController(ProductCategoryService service) { this.service = service; }

    @GetMapping
    public List<ProductCategory> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategory create(@Valid @RequestBody ProductCategoryRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public ProductCategory update(@PathVariable Long id, @Valid @RequestBody ProductCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id) { service.disable(id); }
}
