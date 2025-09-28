package com.aiexpert.vendingbench.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "vending")
public class SimulationConfig {

    private Model model = new Model();
    private Agent agent = new Agent();
    private Simulation simulation = new Simulation();
    private Safety safety = new Safety();

    // Getters for the nested properties
    public Model getModel() { return model; }
    public Agent getAgent() { return agent; }
    public Simulation getSimulation() { return simulation; }
    public Safety getSafety() { return safety; }

    // Nested static classes to match the YAML structure
    public static class Model {
        private String name;
        private String apiEnvVar;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getApiEnvVar() { return apiEnvVar; }
        public void setApiEnvVar(String apiEnvVar) { this.apiEnvVar = apiEnvVar; }
    }

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