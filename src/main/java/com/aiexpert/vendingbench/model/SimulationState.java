package com.aiexpert.vendingbench.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationState {
    private int turn = 1;
    private int day = 1;
    private double cashBalance;
    private double dailyFee;
    private Inventory storage = new Inventory();
    private Inventory vendingMachine = new Inventory();
    private List<Map<String, String>> emailInbox = new ArrayList<>();
    private List<Map<String, String>> sentEmails = new ArrayList<>(); // New field
    private int totalUnitsSold = 0;

    @JsonIgnore
    private Map<Integer, Map<String, Integer>> pendingDeliveries = new ConcurrentHashMap<>();

    public SimulationState(double initialCashBalance, double dailyFee) {
        this.cashBalance = initialCashBalance;
        this.dailyFee = dailyFee;
        storage.addOrUpdateItem("Chips", 50, 1.75, 0.30);
        storage.addOrUpdateItem("Candy", 50, 1.25, 0.25);
        storage.addOrUpdateItem("Soda", 50, 2.00, 0.50);
    }

    public void incrementTurn() { this.turn++; }
    public void incrementDay() {
        this.day++;
        this.cashBalance -= this.dailyFee;
    }

    public void addEmail(Map<String, String> email) { this.emailInbox.add(email); }
    public void clearEmails() { this.emailInbox.clear(); }

    // Method to add sent email
    public void addSentEmail(String recipient, String body) {
        Map<String, String> sentEmail = Map.of(
            "recipient", recipient,
            "body", body,
            "timestamp", Instant.now().toString()
        );
        this.sentEmails.add(sentEmail);
    }

    public void addPendingDelivery(int arrivalDay, Map<String, Integer> items) {
        pendingDeliveries.compute(arrivalDay, (day, existingItems) -> {
            if (existingItems == null) {
                return new ConcurrentHashMap<>(items);
            } else {
                items.forEach((itemName, quantity) -> existingItems.merge(itemName, quantity, Integer::sum));
                return existingItems;
            }
        });
    }

    // --- Getters and Setters ---
    public int getTurn() { return turn; }
    public void setTurn(int turn) { this.turn = turn; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }
    public double getDailyFee() { return dailyFee; }
    public Inventory getStorage() { return storage; }
    public void setStorage(Inventory storage) { this.storage = storage; }
    public Inventory getVendingMachine() { return vendingMachine; }
    public void setVendingMachine(Inventory vendingMachine) { this.vendingMachine = vendingMachine; }
    public List<Map<String, String>> getEmailInbox() { return emailInbox; }
    public void setEmailInbox(List<Map<String, String>> emailInbox) { this.emailInbox = emailInbox; }
    public int getTotalUnitsSold() { return totalUnitsSold; }
    public void setTotalUnitsSold(int totalUnitsSold) { this.totalUnitsSold = totalUnitsSold; }
    public Map<Integer, Map<String, Integer>> getPendingDeliveries() { return pendingDeliveries; }
    public List<Map<String, String>> getSentEmails() { return sentEmails; }
    public void setSentEmails(List<Map<String, String>> sentEmails) { this.sentEmails = sentEmails; }
}