package com.example.hack1.events;

import java.time.LocalDate;

public class PremiumReportRequestedEvent {
    private final String requestId;
    private final LocalDate from;
    private final LocalDate to;
    private final String branch; // null para CENTRAL
    private final String emailTo;
    private final boolean includeCharts;
    private final boolean attachPdf;

    public PremiumReportRequestedEvent(String requestId, LocalDate from, LocalDate to, String branch, String emailTo, boolean includeCharts, boolean attachPdf) {
        this.requestId = requestId;
        this.from = from;
        this.to = to;
        this.branch = branch;
        this.emailTo = emailTo;
        this.includeCharts = includeCharts;
        this.attachPdf = attachPdf;
    }

    public String getRequestId() { return requestId; }
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public String getBranch() { return branch; }
    public String getEmailTo() { return emailTo; }
    public boolean isIncludeCharts() { return includeCharts; }
    public boolean isAttachPdf() { return attachPdf; }
}