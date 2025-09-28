package com.aiexpert.vendingbench.tool.memory;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.service.MemoryManager;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class WriteToMemoryTool implements Tool {
    private final MemoryManager memoryManager;

    public WriteToMemoryTool(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        String memoryType = params.path("memory_type").asText("").toLowerCase();
        String content = params.path("content").asText();

        if (content.isEmpty()) {
            return "Error: 'content' parameter cannot be empty.";
        }

        switch (memoryType) {
            case "scratchpad":
                memoryManager.updateScratchpad(content);
                return "Successfully updated scratchpad with " + content.length() + " characters.";
                
            case "kv_store":
                String key = params.path("key").asText();
                if (key.isEmpty()) {
                    return "Error: 'key' parameter is required for kv_store.";
                }
                memoryManager.saveToKeyValueStore(key, content);
                return "Successfully saved value for key '" + key + "'.";
                
            case "vector_db":
            case "vector_database": // Add this alias to handle both variations
                String id = memoryManager.storeInVectorDB(content);
                return "Successfully stored in vector database with ID: " + id + " (Total entries: " + memoryManager.getVectorDBSize() + ")";
                
            default:
                return "Error: Invalid 'memory_type'. Must be 'scratchpad', 'kv_store', or 'vector_db'.";
        }
    }
}