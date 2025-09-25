package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.environment.EmailSimulation;
import com.aiexpert.vendingbench.llm.LLMService;
import com.aiexpert.vendingbench.llm.LLMServiceProvider;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class ComposeEmailToContactTool implements Tool {
    private final EmailSimulation emailSimulation;
    private final LLMServiceProvider llmServiceProvider;
    private static final Map<String, String> CONTACT_MAP = Map.of(
        "supplier", "supplier@globalsnacks.com",
        "maintenance", "maintenance@vendingtech.com",
        "marketing", "marketing@localads.com",
        "finance", "finance@businessbank.com"
    );

    public ComposeEmailToContactTool(EmailSimulation emailSimulation, LLMServiceProvider llmServiceProvider) {
        this.emailSimulation = emailSimulation;
        this.llmServiceProvider = llmServiceProvider;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        String contactType = params.path("contact_type").asText();
        String topic = params.path("topic").asText();

        if (topic.isBlank() || contactType.isBlank()) {
            return "Error: You must provide a 'contact_type' (e.g., 'supplier') and a 'topic' for the email.";
        }

        String recipient = CONTACT_MAP.get(contactType.toLowerCase());
        if (recipient == null) {
            return "Error: Unknown contact_type '" + contactType + "'. Valid types are: " + String.join(", ", CONTACT_MAP.keySet());
        }

        LLMService llmService = llmServiceProvider.getActiveService();
        String prompt = String.format(
            "You are a business professional writing an email. Your task is to write a concise, professional email body to a %s about the following topic: '%s'. " +
            "The email should be 2-3 sentences. IMPORTANT: Respond ONLY with the raw text of the email body, with no salutation (like 'Dear Sir'), sign-off, or any other surrounding text.",
            contactType, topic
        );
        String body = llmService.generate(prompt);

        if (body == null || body.isBlank() || body.contains("Error:")) {
            return "Error: Failed to compose an email body for the topic: " + topic;
        }

        state.addSentEmail(recipient, body);
        emailSimulation.handleSentEmail(recipient, body, state);

        return "Successfully composed and sent email to " + recipient + " about '" + topic + "'. A reply may arrive later.";
    }
}