package com.aiexpert.vendingbench.agent;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;

import java.util.Map;

public abstract class Agent {
    protected final String name;
    protected String status;

    public Agent(String name) {
        this.name = name;
        this.status = "Idle";
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public abstract Action act(SimulationState state, Map<String, Tool> availableTools);
}