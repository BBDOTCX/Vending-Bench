package com.aiexpert.vendingbench.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Inventory {
    private Map<String, Item> items = new ConcurrentHashMap<>();
    private double cashHeld = 0.0;

    public void addOrUpdateItem(String name, int quantity, double price, double wholesaleCost) {
        items.compute(name, (k, v) -> {
            if (v == null) {
                return new Item(name, quantity, price, wholesaleCost);
            } else {
                v.setQuantity(v.getQuantity() + quantity);
                // Note: Price and wholesaleCost are not updated here to preserve original values
                // unless it's a new item.
                return v;
            }
        });
    }

    public void addItem(Item itemToAdd) {
        items.compute(itemToAdd.getName(), (k, existingItem) -> {
            if (existingItem == null) {
                return new Item(itemToAdd);
            } else {
                existingItem.setQuantity(existingItem.getQuantity() + itemToAdd.getQuantity());
                return existingItem;
            }
        });
    }

    public void removeItem(String itemName, int quantityToRemove) {
        items.computeIfPresent(itemName, (k, item) -> {
            int newQuantity = item.getQuantity() - quantityToRemove;
            item.setQuantity(Math.max(0, newQuantity));
            return item;
        });

        // --- THIS LINE WAS THE BUG. IT HAS BEEN REMOVED. ---
        // items.values().removeIf(item -> item.getQuantity() <= 0);
        // We should not remove items from the map when their quantity is zero,
        // as they still need to exist as a known product type.
    }

    public Map<String, Item> getClonedItems() {
        return this.items.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new Item(e.getValue())));
    }

    public Item getItem(String name) { return items.get(name); }
    public Map<String, Item> getItems() { return items; }
    public void setItems(Map<String, Item> items) { this.items = items; }
    public double getCashHeld() { return cashHeld; }
    public void setCashHeld(double cashHeld) { this.cashHeld = cashHeld; }
    public void addCash(double amount) { this.cashHeld += amount; }
}