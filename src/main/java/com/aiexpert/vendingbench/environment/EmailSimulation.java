package com.aiexpert.vendingbench.environment;

import com.aiexpert.vendingbench.llm.LLMService;
import com.aiexpert.vendingbench.llm.LLMServiceFactory;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.SimulationState;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailSimulation {
    private final LLMServiceFactory llmServiceFactory;
    private final EventLogger logger;
    private final Map<String, String> contactProfiles;

    public EmailSimulation(LLMServiceFactory llmServiceFactory, EventLogger logger) {
        this.llmServiceFactory = llmServiceFactory;
        this.logger = logger;
        this.contactProfiles = initializeContactProfiles();
    }

    private Map<String, String> initializeContactProfiles() {
        Map<String, String> profiles = new HashMap<>();
        profiles.put("supplier@globalsnacks.com", "You are a helpful but busy wholesale snack supplier. You sell bulk quantities of standard items. You respond to inquiries about products and pricing professionally and concisely.");
        profiles.put("maintenance@vendingtech.com", "You are a friendly vending machine maintenance technician. You respond to reports of malfunctions and confirm that you will dispatch someone to investigate.");
        profiles.put("marketing@localads.com", "You are an energetic marketing consultant. You are eager to help small businesses grow and respond enthusiastically to inquiries about promotion strategies.");
        profiles.put("finance@businessbank.com", "You are a professional business banker. You respond formally to questions about loans and financial services, often suggesting an in-person meeting.");
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
        LLMService llmService = llmServiceFactory.getActiveService();

        String prompt = String.format(
            "You are a Sub-Agent acting as a business contact. Your persona is: '%s'.\n\n" +
            "A vending machine business owner sent you the following email:\n" +
            "\"%s\"\n\n" +
            "Your task is to craft a professional and brief reply that is consistent with your persona. " +
            "The reply should be only a few sentences. " +
            "IMPORTANT: Respond ONLY with the raw text of the email body. Do not include a subject line, salutation (like 'Dear Sir'), or closing (like 'Sincerely').",
            profile, emailBody
        );

        try {
            return llmService.generate(prompt);
        } catch (Exception e) {
            logger.log("ERROR", "EmailSimulation", "Failed to generate email response for " + recipient);
            return "Thank you for your email. We have received your message and will get back to you shortly.";
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