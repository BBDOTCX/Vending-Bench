package com.aiexpert.vendingbench.logging;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventLogger {
    private static final int MAX_VERBOSE_LOGS = 1000;
    private static final int MAX_MAIN_LOGS = 200;
    
    private final List<LogEntry> verboseLog = new CopyOnWriteArrayList<>();
    private final List<String> mainLog = new CopyOnWriteArrayList<>();

    public void log(String type, String source, String message, Map<String, Object> details) {
        LogEntry entry = new LogEntry(Instant.now(), type, source, message, details);
        verboseLog.add(entry);
        
        // Rotate verbose logs
        while (verboseLog.size() > MAX_VERBOSE_LOGS) {
            verboseLog.remove(0);
        }

        // Add user-friendly messages to the main event log
        if ("TOOL_RESULT".equals(type) || "SIMULATION_START".equals(type) || "ERROR".equals(type) || "TURN_START".equals(type) || type.startsWith("HUMAN") || "CUSTOMER_SALES".equals(type) || "DELIVERY_RECEIVED".equals(type)) {
            String logMessage = "[" + source + "] " + message;
            if (details != null && details.containsKey("result")) {
                logMessage += ": " + details.get("result");
            } else if (details != null && details.containsKey("details")) {
                 logMessage += ": " + details.get("details");
            }
             addMainLogEntry(logMessage);
        }
    }

    public void log(String type, String source, String message) {
        log(type, source, message, null);
    }
    
    public void addMainLogEntry(String entry) {
        mainLog.add(entry);
        // Rotate main logs
        while (mainLog.size() > MAX_MAIN_LOGS) {
            mainLog.remove(0);
        }
    }

    public List<LogEntry> getVerboseLog() {
        return new ArrayList<>(verboseLog);
    }

    public List<String> getMainLog() {
        return new ArrayList<>(mainLog);
    }

    public void clear() {
        verboseLog.clear();
        mainLog.clear();
    }
}