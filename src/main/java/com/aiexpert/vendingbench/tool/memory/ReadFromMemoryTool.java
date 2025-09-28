package com.aiexpert.vendingbench.tool.memory;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.service.MemoryManager;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class ReadFromMemoryTool implements Tool {
    private final MemoryManager memoryManager;

    public ReadFromMemoryTool(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        String memoryType = params.path("memory_type").asText("").toLowerCase();

        switch (memoryType) {
            case "scratchpad":
                String scratchpadContent = memoryManager.getScratchpad();
                return scratchpadContent.isEmpty() ? "Scratchpad is empty." : "Scratchpad content:\n" + scratchpadContent;
                
            case "kv_store":
                String key = params.path("key").asText();
                if (key.isEmpty()) {
                    // If no key specified, list all keys
                    List<String> keys = memoryManager.getAllKeys();
                    if (keys.isEmpty()) {
                        return "Key-value store is empty.";
                    }
                    return "Available keys: " + String.join(", ", keys);
                }
                return memoryManager.getFromKeyValueStore(key);
                
            case "vector_db":
                String query = params.path("query").asText();
                if (query.isEmpty()) {
                    return "Error: 'query' parameter is required for vector_db search.";
                }
                int limit = params.path("limit").asInt(5);
                List<String> results = memoryManager.searchVectorDB(query, Math.min(limit, 10));
                if (results.isEmpty() || results.get(0).equals("Vector database is empty.")) {
                    return "No relevant memories found for query: " + query;
                }
                return "Relevant memories for '" + query + "':\n" + String.join("\n---\n", results);
                
            default:
                return "Error: Invalid 'memory_type'. Must be 'scratchpad', 'kv_store', or 'vector_db'.";
        }
    }
}