package com.aiexpert.vendingbench.controller.dto;

public class SimulationStartRequest {
    private String provider;
    private String apiKey;
    private String modelName;
    private String persona;
    private int maxTurns;

    // Getters and Setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public int getMaxTurns() { return maxTurns; }
    public void setMaxTurns(int maxTurns) { this.maxTurns = maxTurns; }
}