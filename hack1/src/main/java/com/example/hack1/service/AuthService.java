package com.example.hack1.service;

import com.example.hack1.dto.auth.AuthResponse;
import com.example.hack1.dto.auth.LoginRequest;
import com.example.hack1.dto.auth.RegisterRequest;
import com.example.hack1.entity.Role;
import com.example.hack1.entity.User;
import com.example.hack1.exception.ResourceConflictException;
import com.example.hack1.repository.UserRepository;
import com.example.hack1.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceConflictException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email already in use");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Role.USER
        );
        User saved = userRepository.save(user);
        String token = tokenProvider.generateTokenFromUsername(saved.getUsername());
        return new AuthResponse(token, saved.getUsername(), saved.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );
        String token = tokenProvider.generateToken(auth);
        var principal = (com.example.hack1.security.CustomUserDetails) auth.getPrincipal();
        return new AuthResponse(token, principal.getUsername(), principal.getEmail());
    }
}
