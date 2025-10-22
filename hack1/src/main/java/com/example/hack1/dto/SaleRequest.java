package com.example.hack1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

import java.time.Instant;

public class SaleRequest {
    @NotBlank
    private String sku;

    @Positive
    private int units;

    @DecimalMin(value = "0.0", inclusive = false)
    private double price;

    // CENTRAL puede enviar cualquier branch; BRANCH será forzado a su branch
    private String branch;

    private Instant soldAt; // opcional; si es null, usar now

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Instant getSoldAt() { return soldAt; }
    public void setSoldAt(Instant soldAt) { this.soldAt = soldAt; }
}