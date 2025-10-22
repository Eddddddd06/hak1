package com.example.hack1.controller;

import com.oreo.insight.domain.Role;
import com.oreo.insight.domain.User;
import com.oreo.insight.dto.LoginRequest;
import com.oreo.insight.dto.LoginResponse;
import com.oreo.insight.dto.RegisterRequest;
import com.oreo.insight.dto.UserResponse;
import com.oreo.insight.repository.UserRepository;
import com.oreo.insight.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String usernameLC = req.getUsername().toLowerCase(Locale.ROOT);
        String emailLC = req.getEmail().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(usernameLC)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("username already exists");
        }
        if (userRepository.existsByEmail(emailLC)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already exists");
        }

        Role role;
        try {
            role = Role.valueOf(req.getRole().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("role must be CENTRAL or BRANCH");
        }
        String branch = req.getBranch();
        if (role == Role.BRANCH && (branch == null || branch.isBlank())) {
            return ResponseEntity.badRequest().body("branch is required for BRANCH role");
        }
        if (role == Role.CENTRAL) {
            branch = null; // CENTRAL no usa sucursal
        }

        User u = new User();
        u.setUsername(usernameLC);
        u.setEmail(emailLC);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(role);
        u.setBranch(branch);

        User saved = userRepository.save(u);
        UserResponse resp = new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole().name(),
                saved.getBranch(),
                saved.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var opt = userRepository.findByUsername(req.getUsername().toLowerCase(Locale.ROOT));
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid credentials");
        }
        User u = opt.get();
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid credentials");
        }
        String token = jwtTokenService.createToken(u.getUsername(), u.getRole().name(), u.getBranch());
        LoginResponse resp = new LoginResponse(token, 3600, u.getRole().name(), u.getBranch());
        return ResponseEntity.ok(resp);
    }
}