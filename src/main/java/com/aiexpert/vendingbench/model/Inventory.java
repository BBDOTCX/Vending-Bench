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
                v.setPrice(price);
                v.setWholesaleCost(wholesaleCost);
                return v;
            }
        });
    }

    public void addItem(Item itemToAdd) {
        items.compute(itemToAdd.getName(), (k, existingItem) -> {
            if (existingItem == null) {
                return new Item(itemToAdd); // Use copy constructor
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
        items.values().removeIf(item -> item.getQuantity() <= 0);
    }
    
    /**
     * **FIX: ADDED THIS METHOD**
     * Creates a deep copy of the items map to allow for safe iteration.
     * @return A new Map containing clones of the items.
     */
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