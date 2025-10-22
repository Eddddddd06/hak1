package com.example.hack1.controller;

import com.example.hack1.dto.auth.AuthResponse;
import com.example.hack1.dto.auth.LoginRequest;
import com.example.hack1.dto.auth.RegisterRequest;
import com.example.hack1.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse resp = authService.register(request);
        return ResponseEntity.status(201).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse resp = authService.login(request);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authorization) {
        // endpoint simple que requiere token (SecurityConfig lo protege)
        return ResponseEntity.ok().body("ok");
    }
}
