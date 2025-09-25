package com.aiexpert.vendingbench.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public class OpenAIService implements LLMService {
    private String apiKey;
    private String modelName = "gpt-4-turbo";
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
            throw new IllegalStateException("API Key is not set for OpenAI.");
        }
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + this.apiKey)
                .build();

        Map<String, Object> requestBody = Map.of(
            "model", this.modelName,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "response_format", Map.of("type", "json_object")
        );

        try {
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            System.err.println("OpenAI API call failed: " + e.getMessage());
            return LLMService.createFallbackError("OpenAI", e.getMessage());
        }
    }
}