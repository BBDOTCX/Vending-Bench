package com.aiexpert.vendingbench.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
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

        // Configure HttpClient with a response timeout to prevent hangs
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60)); // Timeout after 60 seconds

        WebClient webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/" + this.modelName + ":generateContent")
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // Apply timeout settings
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
                    .block(); // This call will now fail after 60 seconds if no response is received

            JsonNode rootNode = objectMapper.readTree(response);
            
            // Gemini API returns a JSON object where the desired JSON is a text string inside.
            // We need to extract this text and parse it again.
            String jsonText = rootNode.at("/candidates/0/content/parts/0/text").asText();
            JsonNode innerJson = objectMapper.readTree(jsonText);
            
            return innerJson.toString();

        } catch (Exception e) {
            System.err.println("Gemini API call failed: " + e.getMessage());
            return LLMService.createFallbackError("Gemini", e.getMessage());
        }
    }
}