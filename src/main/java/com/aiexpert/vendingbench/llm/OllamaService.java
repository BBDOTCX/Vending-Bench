package com.aiexpert.vendingbench.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.Map;

public class OllamaService implements LLMService {
    private String modelName = "llama3";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String ollamaApiUrl = "http://localhost:11434/api/generate";

    @Override
    public void configure(String apiKey, String modelName) {
        // API key is ignored for Ollama
        if (modelName != null && !modelName.isBlank()) {
            this.modelName = modelName;
        }
    }

    @Override
    public String generate(String prompt) {
        // Configure HttpClient with a generous response timeout for model loading
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(3)); // Timeout after 3 minutes

        WebClient webClient = WebClient.builder()
                .baseUrl(ollamaApiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // Apply timeout settings
                .build();

        Map<String, Object> requestBody = Map.of(
            "model", this.modelName,
            "prompt", prompt,
            "format", "json",
            "stream", false
        );

        try {
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.path("response").asText();
        } catch (Exception e) {
            System.err.println("Ollama API call failed: " + e.getMessage() + ". Is Ollama running?");
            return LLMService.createFallbackError("Ollama", "Local instance failed. Is it running and is the model '" + modelName + "' pulled?");
        }
    }
}