package com.aiexpert.vendingbench.llm;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class LLMServiceProvider {
    private final Map<String, LLMService> services;
    private LLMService activeService;

    public LLMServiceProvider() {
        this.services = Map.of(
            "gemini", new GeminiService(),
            "anthropic", new AnthropicService(),
            "openai", new OpenAIService(),
            "ollama", new OllamaService()
        );
        this.activeService = services.get("gemini"); // Default to Gemini
    }

    public void setActiveService(String provider, String apiKey, String modelName) {
        if (!services.containsKey(provider)) {
            throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }
        this.activeService = services.get(provider);
        this.activeService.configure(apiKey, modelName);
    }

    public LLMService getActiveService() {
        if (activeService == null) {
            throw new IllegalStateException("No active LLM service has been configured.");
        }
        return activeService;
    }
}