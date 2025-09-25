package com.aiexpert.vendingbench.tool;

import com.aiexpert.vendingbench.model.SimulationState;
import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {
    String execute(JsonNode params, SimulationState state);
}