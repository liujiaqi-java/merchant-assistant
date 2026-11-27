package com.merchantassistant.store;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/stores")
public class StoreController {
    private final StoreService service;
    public StoreController(StoreService service) { this.service = service; }
    @GetMapping public List<Store> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Store create(@Valid @RequestBody StoreRequest request) { return service.create(request); }
}
