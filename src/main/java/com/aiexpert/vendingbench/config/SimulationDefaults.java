package com.aiexpert.vendingbench.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "vending")
public class SimulationDefaults {

    @NestedConfigurationProperty
    private final Agent agent = new Agent();
    @NestedConfigurationProperty
    private final Simulation simulation = new Simulation();
    @NestedConfigurationProperty
    private final Safety safety = new Safety();

    public Agent getAgent() { return agent; }
    public Simulation getSimulation() { return simulation; }
    public Safety getSafety() { return safety; }

    public static class Agent {
        private int maxContextTokens;
        public int getMaxContextTokens() { return maxContextTokens; }
        public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }
    }

    public static class Simulation {
        private double initialCashBalance;
        private double dailyFee;
        private int turnDelayMs;
        private int maxTurns;
        private int maxVendingMachineCapacity;
        private double minItemPrice;
        private double maxItemPrice;

        public double getInitialCashBalance() { return initialCashBalance; }
        public void setInitialCashBalance(double initialCashBalance) { this.initialCashBalance = initialCashBalance; }
        public double getDailyFee() { return dailyFee; }
        public void setDailyFee(double dailyFee) { this.dailyFee = dailyFee; }
        public int getTurnDelayMs() { return turnDelayMs; }
        public void setTurnDelayMs(int turnDelayMs) { this.turnDelayMs = turnDelayMs; }
        public int getMaxTurns() { return maxTurns; }
        public void setMaxTurns(int maxTurns) { this.maxTurns = maxTurns; }
        public int getMaxVendingMachineCapacity() { return maxVendingMachineCapacity; }
        public void setMaxVendingMachineCapacity(int maxVendingMachineCapacity) { this.maxVendingMachineCapacity = maxVendingMachineCapacity; }
        public double getMinItemPrice() { return minItemPrice; }
        public void setMinItemPrice(double minItemPrice) { this.minItemPrice = minItemPrice; }
        public double getMaxItemPrice() { return maxItemPrice; }
        public void setMaxItemPrice(double maxItemPrice) { this.maxItemPrice = maxItemPrice; }
    }

    public static class Safety {
        private int meltdownRepeatThreshold;
        private int meltdownWindow;
        public int getMeltdownRepeatThreshold() { return meltdownRepeatThreshold; }
        public void setMeltdownRepeatThreshold(int meltdownRepeatThreshold) { this.meltdownRepeatThreshold = meltdownRepeatThreshold; }
        public int getMeltdownWindow() { return meltdownWindow; }
        public void setMeltdownWindow(int meltdownWindow) { this.meltdownWindow = meltdownWindow; }
    }
}