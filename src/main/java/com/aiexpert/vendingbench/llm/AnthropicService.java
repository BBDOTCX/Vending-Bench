package com.aiexpert.vendingbench.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public class AnthropicService implements LLMService {
    private String apiKey;
    private String modelName = "claude-3-haiku-20240307";
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
            throw new IllegalStateException("API Key is not set for Anthropic.");
        }
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com/v1/messages")
                .defaultHeader("x-api-key", this.apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        
        Map<String, Object> requestBody = Map.of(
            "model", this.modelName,
            "max_tokens", 4096,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.at("/content/0/text").asText();
        } catch (Exception e) {
            System.err.println("Anthropic API call failed: " + e.getMessage());
            return LLMService.createFallbackError("Anthropic", e.getMessage());
        }
    }
}