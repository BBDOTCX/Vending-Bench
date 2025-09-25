package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.environment.EmailSimulation;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class SendEmailTool implements Tool {
    private final EmailSimulation emailSimulation;

    public SendEmailTool(EmailSimulation emailSimulation) {
        this.emailSimulation = emailSimulation;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        String recipient = params.path("recipient").asText();
        String body = params.path("body").asText();

        if (recipient.isBlank() || body.isBlank()) {
            return "Error: Email recipient and body cannot be empty.";
        }

        // Track the sent email
        state.addSentEmail(recipient, body);

        // Handle the email (which also generates the response)
        emailSimulation.handleSentEmail(recipient, body, state);

        return "Email sent to " + recipient + ". A reply may arrive in your inbox later.";
    }
}