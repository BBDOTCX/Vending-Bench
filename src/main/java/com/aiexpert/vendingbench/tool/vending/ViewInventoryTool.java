package com.aiexpert.vendingbench.tool.vending;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ViewInventoryTool implements Tool {
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String execute(JsonNode params, SimulationState state) {
        try {
            // Note: The pretty-printed JSON is intentionally verbose.
            // It provides the agent with a complete and easily parsable view of its inventory.
            // The frontend filters this specific message from the main log to avoid clutter.
            String storageJson = objectMapper.writeValueAsString(state.getStorage());
            String machineJson = objectMapper.writeValueAsString(state.getVendingMachine());
            return "Current Inventories:\n---STORAGE---\n" + storageJson + "\n---VENDING MACHINE---\n" + machineJson;
        } catch (JsonProcessingException e) {
            return "Error: Could not serialize inventory data.";
        }
    }
}