package com.aiexpert.vendingbench.agent;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubAgent extends Agent {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubAgent() {
        super("SubAgent");
        this.status = "Idle";
    }

    /**
     * Executes a given action by finding the corresponding tool and running it.
     * This agent does not use an LLM; it is a direct executor.
     * @param action The action plan received from the MainAgent.
     * @param state The current simulation state.
     * @param availableTools A map of all available tools.
     * @return The string result of the tool's execution.
     */
    public String execute(Action action, SimulationState state, Map<String, Tool> availableTools) {
        String toolName = action.toolName();
        this.status = "Executing tool: " + toolName;

        if (availableTools.containsKey(toolName)) {
            try {
                Tool tool = availableTools.get(toolName);
                String result = tool.execute(objectMapper.readTree(action.parameters()), state);
                this.status = "Idle";
                return result;
            } catch (Exception e) {
                this.status = "Idle (Execution Error)";
                return "Error executing tool '" + toolName + "': " + e.getMessage();
            }
        } else {
            this.status = "Idle (Unknown Tool)";
            return "Error: SubAgent was asked to execute an unknown tool: " + toolName;
        }
    }

    @Override
    public Action act(SimulationState state, Map<String, Tool> availableTools) {
        // This agent does not decide on actions, it only executes them.
        return null;
    }
}