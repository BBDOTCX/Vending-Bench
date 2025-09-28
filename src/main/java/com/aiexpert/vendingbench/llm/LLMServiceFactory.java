package com.aiexpert.vendingbench.llm;

import org.springframework.stereotype.Service;

@Service
public class LLMServiceFactory {
    
    private LLMService activeService;

    public LLMService getService(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("LLM provider name cannot be null or blank.");
        }

        return switch (provider.toLowerCase()) {
            case "gemini" -> new GeminiService();
            case "anthropic" -> new AnthropicService();
            case "openai" -> new OpenAIService();
            case "ollama" -> new OllamaService();
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }
    
    public void configureActiveService(String provider, String apiKey, String modelName) {
        this.activeService = getService(provider);
        this.activeService.configure(apiKey, modelName);
    }

    public LLMService getActiveService() {
        if (activeService == null) {
            throw new IllegalStateException("No active LLM service has been configured.");
        }
        return activeService;
    }
}