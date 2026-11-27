package com.merchantassistant.product;

import jakarta.validation.Valid;
import com.merchantassistant.common.PageResponse;
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
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String keyword) {
        return service.list(keyword);
    }

    @GetMapping("/page")
    public PageResponse<Product> page(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) Boolean enabled,
                                      @RequestParam(defaultValue = "false") boolean lowStock) {
        return service.page(page, size, keyword, category, enabled, lowStock);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
