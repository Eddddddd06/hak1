package com.example.hack1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class WeeklyPremiumRequest {
    // Opcionales: si no se envían, se calcula última semana
    private LocalDate from;
    private LocalDate to;

    // Opcional para CENTRAL; para BRANCH, si es null se usa su sucursal
    private String branch;

    @NotBlank
    @Email
    private String emailTo;

    // "PREMIUM" esperado; si se envía otro valor, se puede ignorar o normalizar
    @Pattern(regexp = "(?i)PREMIUM")
    private String format = "PREMIUM";

    // Flags
    private boolean includeCharts = true;
    private boolean attachPdf = true;

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getEmailTo() { return emailTo; }
    public void setEmailTo(String emailTo) { this.emailTo = emailTo; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public boolean isIncludeCharts() { return includeCharts; }
    public void setIncludeCharts(boolean includeCharts) { this.includeCharts = includeCharts; }
    public boolean isAttachPdf() { return attachPdf; }
    public void setAttachPdf(boolean attachPdf) { this.attachPdf = attachPdf; }
}