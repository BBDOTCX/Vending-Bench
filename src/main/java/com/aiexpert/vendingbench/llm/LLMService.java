package com.aiexpert.vendingbench.llm;

public interface LLMService {
    void configure(String apiKey, String modelName);
    String generate(String prompt);

    /**
     * Creates a standardized JSON fallback response when an API call fails.
     * This ensures the agent can understand the failure and ask for help.
     * @param provider The name of the LLM provider that failed.
     * @param errorDetails A descriptive error message.
     * @return A JSON string representing the fallback action.
     */
    static String createFallbackError(String provider, String errorDetails) {
        String sanitizedDetails = errorDetails.replace("\"", "'"); // Sanitize quotes
        return String.format(
            "{\"thought\": \"The API call to %s failed with an error: %s. This could be due to an invalid API key, network issues, or a problem with the API itself. I should ask a human for help if this continues.\", \"action\": {\"tool\": \"ask_for_human_help\", \"parameters\": {}}}",
            provider,
            sanitizedDetails
        );
    }
}