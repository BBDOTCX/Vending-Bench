package com.aiexpert.vendingbench.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String apiKey;
    private final String model = "text-embedding-3-small";

    public void configure(String openaiApiKey) {
        this.apiKey = openaiApiKey;
    }

    public float[] createEmbedding(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            // Return dummy embedding if no API key (for testing)
            return createDummyEmbedding(text);
        }

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/embeddings")
                .defaultHeader("Authorization", "Bearer " + this.apiKey)
                .build();

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "input", text
        );

        try {
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode embeddingNode = rootNode.at("/data/0/embedding");
            
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            System.err.println("Embedding API call failed: " + e.getMessage());
            return createDummyEmbedding(text);
        }
    }

    private float[] createDummyEmbedding(String text) {
        // Simple hash-based dummy embedding for testing without API key
        int hash = text.hashCode();
        float[] embedding = new float[1536]; // Same dimension as text-embedding-3-small
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) Math.sin(hash + i) * 0.1f;
        }
        return embedding;
    }

    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length && i < vectorB.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}