package com.aiexpert.vendingbench.environment;

import com.aiexpert.vendingbench.llm.LLMService;
import com.aiexpert.vendingbench.llm.LLMServiceProvider;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.SimulationState;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailSimulation {
    private final LLMServiceProvider llmServiceProvider;
    private final EventLogger logger;
    private final Map<String, String> contactProfiles;

    public EmailSimulation(LLMServiceProvider llmServiceProvider, EventLogger logger) {
        this.llmServiceProvider = llmServiceProvider;
        this.logger = logger;
        this.contactProfiles = initializeContactProfiles();
    }

    private Map<String, String> initializeContactProfiles() {
        Map<String, String> profiles = new HashMap<>();
        profiles.put("supplier@globalsnacks.com", "You are a wholesale snack supplier. You sell bulk quantities of chips, candy, and beverages. You're business-focused and provide pricing information.");
        profiles.put("maintenance@vendingtech.com", "You are a vending machine maintenance technician. You help with technical issues, repairs, and equipment upgrades.");
        profiles.put("marketing@localads.com", "You are a marketing consultant specializing in small business promotion and customer acquisition strategies.");
        profiles.put("finance@businessbank.com", "You are a business banker who helps with loans, financial advice, and business accounts.");
        return profiles;
    }

    public void handleSentEmail(String recipient, String body, SimulationState state) {
        if (recipient == null || recipient.trim().isEmpty() || body == null || body.trim().isEmpty()) {
            logger.log("ERROR", "EmailSimulation", "Email recipient or body is empty. Aborting email sending.");
            return;
        }

        logger.log("EMAIL_SENT", "MainAgent", "Email sent to " + recipient,
                       Map.of("body", body, "recipient", recipient));

        String replyBody = generateResponse(recipient, body);
        
        if (replyBody != null && !replyBody.isEmpty()) {
            Map<String, String> replyEmail = Map.of(
                "sender", recipient,
                "body", replyBody
            );
            
            state.addEmail(replyEmail);
            logger.log("EMAIL_RECEIVED", "EmailSimulation",
                      "Reply received from " + recipient, Map.of("body", replyBody));
        }
    }

    private String generateResponse(String recipient, String emailBody) {
        String profile = getContactProfile(recipient);
        LLMService llmService = llmServiceProvider.getActiveService();

        // REVISED PROMPT for clarity and conciseness
        String prompt = String.format(
            "%s\n\n" +
            "A vending machine business owner sent you the following email:\n\n" +
            "\"%s\"\n\n" +
            "Your task is to respond professionally and concisely. Your response should be a few sentences long, directly addressing their inquiry. Do not include a subject line, salutation, or closing. Respond ONLY with the email body text.",
            profile, emailBody
        );

        try {
            return llmService.generate(prompt);
        } catch (Exception e) {
            logger.log("ERROR", "EmailSimulation", "Failed to generate email response for " + recipient);
            return "Thank you for your email. I'll get back to you soon with more information.";
        }
    }

    private String getContactProfile(String recipient) {
        String recipientLower = recipient.toLowerCase();
        if (contactProfiles.containsKey(recipientLower)) {
            return contactProfiles.get(recipientLower);
        }
        
        if (recipientLower.contains("supplier")) {
            return contactProfiles.get("supplier@globalsnacks.com");
        } else if (recipientLower.contains("maintenance") || recipientLower.contains("tech")) {
            return contactProfiles.get("maintenance@vendingtech.com");
        } else if (recipientLower.contains("marketing") || recipientLower.contains("ads")) {
            return contactProfiles.get("marketing@localads.com");
        } else if (recipientLower.contains("bank") || recipientLower.contains("finance")) {
            return contactProfiles.get("finance@businessbank.com");
        } else {
            return "You are a helpful business professional. Respond to inquiries in a professional manner and offer assistance where appropriate.";
        }
    }
}