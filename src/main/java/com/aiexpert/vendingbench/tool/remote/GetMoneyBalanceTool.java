package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class GetMoneyBalanceTool implements Tool {
    @Override
    public String execute(JsonNode params, SimulationState state) {
        return String.format("Your current main cash balance is: $%.2f", state.getCashBalance());
    }
}