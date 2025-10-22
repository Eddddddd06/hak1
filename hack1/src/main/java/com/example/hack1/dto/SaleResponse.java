package com.example.hack1.dto;

import java.time.Instant;

public class SaleResponse {
    private String id;
    private String sku;
    private int units;
    private double price;
    private String branch;
    private Instant soldAt;
    private String createdBy;

    public SaleResponse(String id, String sku, int units, double price, String branch, Instant soldAt, String createdBy) {
        this.id = id;
        this.sku = sku;
        this.units = units;
        this.price = price;
        this.branch = branch;
        this.soldAt = soldAt;
        this.createdBy = createdBy;
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public int getUnits() { return units; }
    public double getPrice() { return price; }
    public String getBranch() { return branch; }
    public Instant getSoldAt() { return soldAt; }
    public String getCreatedBy() { return createdBy; }
}