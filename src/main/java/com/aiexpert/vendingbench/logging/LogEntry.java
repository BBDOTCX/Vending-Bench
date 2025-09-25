package com.aiexpert.vendingbench.logging;

import java.time.Instant;
import java.util.Map;

public class LogEntry {
    private final Instant timestamp;
    private final String type;
    private final String source;
    private final String message;
    private final Map<String, Object> details;

    public LogEntry(Instant timestamp, String type, String source, String message, Map<String, Object> details) {
        this.timestamp = timestamp;
        this.type = type;
        this.source = source;
        this.message = message;
        this.details = details;
    }

    // Getters
    public Instant getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getSource() { return source; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }
}