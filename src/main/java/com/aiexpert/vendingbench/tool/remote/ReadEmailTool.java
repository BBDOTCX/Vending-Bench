package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReadEmailTool implements Tool {
    @Override
    public String execute(JsonNode params, SimulationState state) {
        List<Map<String, String>> inbox = state.getEmailInbox();

        if (inbox.isEmpty()) {
            return "Your email inbox is empty.";
        }

        AtomicInteger counter = new AtomicInteger(1);
        String allEmails = inbox.stream()
            .map(email -> String.format(
                "--- EMAIL %d ---\nFrom: %s\nBody: %s\n",
                counter.getAndIncrement(),
                email.get("sender"),
                email.get("body")
            ))
            .collect(Collectors.joining("\n"));
        
        // Reading emails consumes them
        state.clearEmails();

        return "You have read all emails from your inbox. The content was:\n\n" + allEmails;
    }
}