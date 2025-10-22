package com.example.hack1.dto;

import java.time.Instant;

public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String branch;
    private Instant createdAt;

    public UserResponse(String id, String username, String email, String role, String branch, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.branch = branch;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getBranch() { return branch; }
    public Instant getCreatedAt() { return createdAt; }
}