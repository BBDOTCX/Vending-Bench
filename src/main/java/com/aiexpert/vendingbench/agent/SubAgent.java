package com.aiexpert.vendingbench.agent;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import java.util.Map;

public class SubAgent extends Agent {
    public SubAgent(String name) {
        super(name);
    }

    @Override
    public Action act(SimulationState state, Map<String, Tool> availableTools) {
        this.status = "Idle";
        return new Action("idle", "{}");
    }
}