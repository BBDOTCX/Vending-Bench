package com.aiexpert.vendingbench.util;

public class ValidationUtils {
    
    public static void validateItemName(String name) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Item name cannot exceed 50 characters");
        }
    }
    
    public static void validatePrice(double price, double min, double max) throws IllegalArgumentException {
        if (Double.isNaN(price) || Double.isInfinite(price)) {
            throw new IllegalArgumentException("Price must be a valid number");
        }
        if (price < min || price > max) {
            throw new IllegalArgumentException(
                String.format("Price %.2f is not within valid range (%.2f - %.2f)", price, min, max)
            );
        }
    }
    
    public static void validateQuantity(int quantity) throws IllegalArgumentException {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (quantity > 10000) {
            throw new IllegalArgumentException("Quantity cannot exceed 10,000 units");
        }
    }
    
    public static void validateEmail(String email) throws IllegalArgumentException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Email address must be in valid format");
        }
    }
}