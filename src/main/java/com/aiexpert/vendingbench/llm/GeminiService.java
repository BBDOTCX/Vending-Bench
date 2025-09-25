package com.aiexpert.vendingbench.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

public class GeminiService implements LLMService {

    private String apiKey;
    private String modelName = "gemini-1.5-flash-latest";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(String apiKey, String modelName) {
        this.apiKey = apiKey;
        if (modelName != null && !modelName.isBlank()) {
            this.modelName = modelName;
        }
    }

    @Override
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key is not set for Gemini.");
        }
        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/" + this.modelName + ":generateContent")
                .build();

        Map<String, Object> requestBody = Map.of(
            "contents", new Map[]{ Map.of("parts", new Map[]{ Map.of("text", prompt) }) },
            "generationConfig", Map.of("responseMimeType", "application/json")
        );

        try {
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.at("/candidates/0/content/parts/0/text").asText();
        } catch (Exception e) {
            System.err.println("Gemini API call failed: " + e.getMessage());
            return LLMService.createFallbackError("Gemini", e.getMessage());
        }
    }
}