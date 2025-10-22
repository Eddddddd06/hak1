package com.example.hack1.dto;

public class LoginResponse {
    private String token;
    private int expiresIn;
    private String role;
    private String branch;

    public LoginResponse(String token, int expiresIn, String role, String branch) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.role = role;
        this.branch = branch;
    }

    public String getToken() { return token; }
    public int getExpiresIn() { return expiresIn; }
    public String getRole() { return role; }
    public String getBranch() { return branch; }
}