package com.aiexpert.vendingbench.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimulationConfig {

    @Value("${vending.simulation.initial-cash-balance}")
    private double initialCashBalance;

    @Value("${vending.simulation.daily-fee}")
    private double dailyFee;

    @Value("${vending.simulation.turn-delay-ms}")
    private int turnDelayMs;

    @Value("${vending.simulation.max-turns}")
    private int maxTurns;
    
    // Values from previous updates
    private final double maxItemPrice = 50.0;
    private final double minItemPrice = 0.01;
    private final int maxInventoryPerItem = 1000;
    private final int maxVendingMachineCapacity = 200;

    // Getters for the new injected values
    public double getInitialCashBalance() { return initialCashBalance; }
    public double getDailyFee() { return dailyFee; }
    public int getTurnDelayMs() { return turnDelayMs; }
    public int getMaxTurns() { return maxTurns; }

    // Getters for the other values
    public double getMaxItemPrice() { return maxItemPrice; }
    public double getMinItemPrice() { return minItemPrice; }
    public int getMaxInventoryPerItem() { return maxInventoryPerItem; }
    public int getMaxVendingMachineCapacity() { return maxVendingMachineCapacity; }
}