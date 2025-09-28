package com.aiexpert.vendingbench.model;

public record VectorMemoryEntry(String id, String text, float[] embedding, long timestamp) {
    public VectorMemoryEntry(String id, String text, float[] embedding) {
        this(id, text, embedding, System.currentTimeMillis());
    }
}