package com.merchantassistant.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {
    private final UserManagementService service;
    public UserManagementController(UserManagementService service) { this.service = service; }
    @GetMapping public List<UserView> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserView create(@Valid @RequestBody CreateUserRequest request) { return service.create(request); }
    @PutMapping("/{id}/status") public UserView status(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request, HttpServletRequest servletRequest) {
        SystemUser operator = (SystemUser) servletRequest.getAttribute("auth.user"); return service.status(id, operator.getId(), request);
    }
    @PostMapping("/{id}/reset-password") public ResponseEntity<Void> reset(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) { service.resetPassword(id, request); return ResponseEntity.noContent().build(); }
}
