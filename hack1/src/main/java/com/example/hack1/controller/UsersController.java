package com.example.hack1.controller;

import com.oreo.insight.domain.User;
import com.oreo.insight.dto.UserResponse;
import com.oreo.insight.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserRepository userRepository;

    public UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(toResponse(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", "user not found")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<?> deleteById(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", "user not found"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole().name(),
                u.getBranch(),
                u.getCreatedAt()
        );
    }

    private ErrorMessage error(String code, String message) {
        return new ErrorMessage(code, message);
    }

    private record ErrorMessage(String code, String message) {}
}