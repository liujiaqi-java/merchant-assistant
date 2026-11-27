package com.merchantassistant.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @PostMapping("/register") public ResponseEntity<UserView> register(@Valid @RequestBody RegisterRequest request) { return ResponseEntity.status(201).body(service.register(request)); }
    @PostMapping("/recover-password") public ResponseEntity<Void> recoverPassword(@Valid @RequestBody RecoverPasswordRequest request) { service.recoverPassword(request); return ResponseEntity.noContent().build(); }
    @PostMapping("/recovery-code") public ResponseEntity<Void> setRecoveryCode(@RequestHeader(value = "X-Session-Token", required = false) String token,
                                                                                 @Valid @RequestBody SetRecoveryCodeRequest request) { service.setRecoveryCode(token, request); return ResponseEntity.noContent().build(); }
    @GetMapping("/me") public UserView me(@RequestHeader(value = "X-Session-Token", required = false) String token) { return service.current(token); }
    @PostMapping("/logout") public ResponseEntity<Void> logout(@RequestHeader(value = "X-Session-Token", required = false) String token) { service.logout(token); return ResponseEntity.noContent().build(); }
    @PostMapping("/change-password") public ResponseEntity<Void> changePassword(@RequestHeader(value = "X-Session-Token", required = false) String token,
                                                                                 @Valid @RequestBody ChangePasswordRequest request) { service.changePassword(token, request); return ResponseEntity.noContent().build(); }
}
