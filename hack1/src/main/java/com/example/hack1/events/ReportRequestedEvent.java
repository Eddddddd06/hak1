package com.example.hack1.events;

import java.time.LocalDate;

public class ReportRequestedEvent {
    private final String requestId;
    private final LocalDate from;
    private final LocalDate to;
    private final String branch; // puede ser null para CENTRAL
    private final String emailTo;

    public ReportRequestedEvent(String requestId, LocalDate from, LocalDate to, String branch, String emailTo) {
        this.requestId = requestId;
        this.from = from;
        this.to = to;
        this.branch = branch;
        this.emailTo = emailTo;
    }

    public String getRequestId() { return requestId; }
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public String getBranch() { return branch; }
    public String getEmailTo() { return emailTo; }
}