package com.aiexpert.vendingbench.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class SimulationConfig {
    public double getInitialCashBalance() { return 500.0; }
    public double getDailyFee() { return 2.0; }
    public int getMaxTurns() { return 100; }
    public int getTurnDelayMs() { return 1000; }
    
    // New validation configuration
    public double getMaxItemPrice() { return 50.0; }
    public double getMinItemPrice() { return 0.01; }
    public int getMaxInventoryPerItem() { return 1000; }
    public int getMaxVendingMachineCapacity() { return 200; }
}