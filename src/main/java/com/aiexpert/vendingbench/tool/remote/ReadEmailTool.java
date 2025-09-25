package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadEmailTool implements Tool {
    @Override
    public String execute(JsonNode params, SimulationState state) {
        List<Map<String, String>> inbox = state.getEmailInbox();

        if (inbox.isEmpty()) {
            return "Your email inbox is empty.";
        }

        String allEmails = inbox.stream()
            .map(email -> "From: " + email.get("sender") + "\nBody:\n" + email.get("body"))
            .collect(Collectors.joining("\n\n---\n\n"));
        
        // Reading emails consumes them
        state.clearEmails();

        return "You have read all emails. The inbox is now empty. The content was:\n\n" + allEmails;
    }
}