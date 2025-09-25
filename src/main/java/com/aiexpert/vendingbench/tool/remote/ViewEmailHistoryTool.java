package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class ViewEmailHistoryTool implements Tool {
    @Override
    public String execute(JsonNode params, SimulationState state) {
        StringBuilder report = new StringBuilder("Email History:\n\n");

        report.append("=== SENT EMAILS ===\n");
        if (state.getSentEmails().isEmpty()) {
            report.append("No emails have been sent.\n");
        } else {
            for (Map<String, String> email : state.getSentEmails()) {
                report.append(String.format("To: %s\nBody: %s\n---\n",
                    email.get("recipient"), email.get("body")));
            }
        }

        report.append("\n=== INBOX ===\n");
        if (state.getEmailInbox().isEmpty()) {
            report.append("The inbox is empty.\n");
        } else {
            for (Map<String, String> email : state.getEmailInbox()) {
                report.append(String.format("From: %s\nBody: %s\n---\n",
                    email.get("sender"), email.get("body")));
            }
        }

        return report.toString();
    }
}