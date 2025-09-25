package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.environment.CustomerSimulation;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class WaitForNextDayTool implements Tool {

    private final CustomerSimulation customerSimulation;

    public WaitForNextDayTool(CustomerSimulation customerSimulation) {
        this.customerSimulation = customerSimulation;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        state.incrementDay();
        String salesReport = customerSimulation.runDailySales(state);
        return "Advanced to Day " + state.getDay() + ". Daily fee of $" + state.getDailyFee() + " applied. " + salesReport;
    }
}