package com.aiexpert.vendingbench.service;

import com.aiexpert.vendingbench.model.VectorMemoryEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MemoryManager {
    private StringBuilder scratchpad;
    private final Map<String, String> keyValueStore;
    private final List<VectorMemoryEntry> vectorDatabase;
    private final EmbeddingService embeddingService;
    
    private static final int MAX_VECTOR_ENTRIES = 1000;
    private long nextId = 1;

    public MemoryManager(EmbeddingService embeddingService) {
        this.scratchpad = new StringBuilder();
        this.keyValueStore = new ConcurrentHashMap<>();
        this.vectorDatabase = new ArrayList<>();
        this.embeddingService = embeddingService;
    }

    public void configureEmbeddings(String openaiApiKey) {
        embeddingService.configure(openaiApiKey);
    }

    public void reset() {
        this.scratchpad = new StringBuilder();
        this.keyValueStore.clear();
        // Vector database can optionally persist across resets for long-term memory
        // this.vectorDatabase.clear();
    }

    // --- Scratchpad Methods ---
    public String getScratchpad() {
        return scratchpad.toString();
    }

    public void updateScratchpad(String content) {
        this.scratchpad.setLength(0);
        this.scratchpad.append(content);
    }

    // --- Key-Value Store Methods ---
    public void saveToKeyValueStore(String key, String value) {
        keyValueStore.put(key, value);
    }

    public String getFromKeyValueStore(String key) {
        return keyValueStore.getOrDefault(key, "No value found for key: " + key);
    }

    public List<String> getAllKeys() {
        return new ArrayList<>(keyValueStore.keySet());
    }

    // --- Vector Database Methods ---
    public String storeInVectorDB(String text) {
        String id = String.valueOf(nextId++);
        float[] embedding = embeddingService.createEmbedding(text);
        
        VectorMemoryEntry entry = new VectorMemoryEntry(id, text, embedding);
        vectorDatabase.add(entry);
        
        // Prune old entries if we exceed maximum
        if (vectorDatabase.size() > MAX_VECTOR_ENTRIES) {
            vectorDatabase.remove(0);
        }
        
        return id;
    }

    public List<String> searchVectorDB(String query, int topK) {
        if (vectorDatabase.isEmpty()) {
            return List.of("Vector database is empty.");
        }
        
        float[] queryEmbedding = embeddingService.createEmbedding(query);
        
        return vectorDatabase.stream()
                .sorted((a, b) -> Double.compare(
                    EmbeddingService.cosineSimilarity(b.embedding(), queryEmbedding),
                    EmbeddingService.cosineSimilarity(a.embedding(), queryEmbedding)
                ))
                .limit(topK)
                .map(entry -> entry.text())
                .collect(Collectors.toList());
    }

    public int getVectorDBSize() {
        return vectorDatabase.size();
    }
}