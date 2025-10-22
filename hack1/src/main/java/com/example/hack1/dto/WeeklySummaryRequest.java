package com.example.hack1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class WeeklySummaryRequest {
    private LocalDate from; // opcional: si no viene, se calcula última semana
    private LocalDate to;   // opcional: si no viene, se calcula última semana
    private String branch;  // opcional
    @NotNull
    @NotBlank
    @Email
    private String emailTo; // obligatorio según README

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getEmailTo() { return emailTo; }
    public void setEmailTo(String emailTo) { this.emailTo = emailTo; }
}