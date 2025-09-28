package com.aiexpert.vendingbench.agent;

import com.aiexpert.vendingbench.llm.LLMService;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.service.MemoryManager;
import com.aiexpert.vendingbench.service.SimulationEngine;
import com.aiexpert.vendingbench.service.TokenizerService;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainAgent extends Agent {

    private record Turn(String thought, String actionJson, String result, int tokenCount) {}

    private final LLMService llmService;
    private final EventLogger logger;
    private final TokenizerService tokenizerService;
    private final MemoryManager memoryManager;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final List<Turn> history = new ArrayList<>();
    private String persona;
    private int maxContextTokens = 30000; // Match research exactly
    private Action humanOverrideAction = null;
    
    private String lastThoughtForHistory = "";
    private String lastActionJsonForHistory = "{}";

    public MainAgent(String name, LLMService llmService, EventLogger logger, TokenizerService tokenizerService, MemoryManager memoryManager) {
        super(name);
        this.llmService = llmService;
        this.logger = logger;
        this.tokenizerService = tokenizerService;
        this.memoryManager = memoryManager;
        this.persona = "A meticulous and profit-oriented business manager focused on long-term growth.";
    }

    public void setHumanOverrideAction(Action action) { this.humanOverrideAction = action; }
    public void setPersona(String persona) { this.persona = persona; }

    public String getLastThought() {
        if (history.isEmpty()) {
            return "Waiting for first turn.";
        }
        return history.get(history.size() - 1).thought();
    }

    @Override
    public Action act(SimulationState state, Map<String, Tool> availableTools) {
        if (humanOverrideAction != null) {
            Action actionToExecute = humanOverrideAction;
            this.status = "Delegating human input";
            this.humanOverrideAction = null;
            return actionToExecute;
        }

        if (llmService == null) {
            this.status = "Idle (No LLM)";
            return new Action(SimulationEngine.IDLE_TOOL, "{}");
        }

        String perception = perceive(state, availableTools);
        logger.log("LLM_REQUEST", name, "Sending prompt to LLM.", Map.of("prompt", perception));

        this.status = "Thinking...";
        String responseJson = llmService.generate(perception);
        logger.log("LLM_RESPONSE", name, "Received response from LLM.", Map.of("response", responseJson));

        try {
            JsonNode responseNode = objectMapper.readTree(responseJson);
            this.lastThoughtForHistory = responseNode.has("thought") ? responseNode.get("thought").asText() : "No thought provided.";
            
            JsonNode actionNode = responseNode.get("action");
            if (actionNode == null) throw new IllegalArgumentException("Response missing 'action' object.");
            
            this.lastActionJsonForHistory = objectMapper.writeValueAsString(actionNode);
            
            String toolName = actionNode.get("tool").asText();
            JsonNode paramsNode = actionNode.get("parameters");
            String paramsJson = (paramsNode != null) ? objectMapper.writeValueAsString(paramsNode) : "{}";
            
            this.status = "Delegating to SubAgent";
            return new Action(toolName, paramsJson);

        } catch (Exception e) {
            this.status = "Idle (Parsing Error)";
            String error = "Error: Could not parse LLM response. Ensure it's valid JSON. Response: " + responseJson;
            updateHistory(error);
            System.err.println("Error parsing LLM response: " + e.getMessage());
            return new Action(SimulationEngine.IDLE_TOOL, "{}");
        }
    }

    public void updateHistory(String result) {
        int turnTokens = tokenizerService.countTokens(this.lastThoughtForHistory + this.lastActionJsonForHistory + result);
        this.history.add(new Turn(this.lastThoughtForHistory, this.lastActionJsonForHistory, result, turnTokens));
        pruneHistory();
        
        this.lastThoughtForHistory = "";
        this.lastActionJsonForHistory = "{}";
    }
    
    private void pruneHistory() {
        int currentTotalTokens = history.stream().mapToInt(Turn::tokenCount).sum();
        if (currentTotalTokens <= maxContextTokens) {
            return;
        }

        List<Turn> turnsToRemove = new ArrayList<>();
        StringBuilder summaryBuilder = new StringBuilder("Summary of oldest events that were pruned from context: ");
        int tokensToPrune = currentTotalTokens - maxContextTokens;
        int tokensPruned = 0;

        for (int i = 0; i < history.size() && tokensPruned < tokensToPrune; i++) {
            Turn turn = history.get(i);
            
            if (turn.actionJson().contains("purchase_from_supplier")) {
                summaryBuilder.append("An order was placed with result: '").append(turn.result()).append("'. ");
            } else if (turn.actionJson().contains("wait_for_next_day")) {
                 summaryBuilder.append("A day passed. ");
            }

            tokensPruned += turn.tokenCount();
            turnsToRemove.add(turn);
        }

        history.removeAll(turnsToRemove);

        String summary = summaryBuilder.toString();
        int summaryTokens = tokenizerService.countTokens(summary);
        history.add(0, new Turn("Summary of pruned history.", "{}", summary, summaryTokens));
        
        logger.log("CONTEXT_PRUNED", name, "Context history pruned and summarized.", Map.of("tokens_removed", tokensPruned));
    }
    
    private String perceive(SimulationState state, Map<String, Tool> availableTools) {
        try {
            String stateJson = objectMapper.writeValueAsString(state);
            String toolList = availableTools.keySet().stream()
                    .map(name -> "\"" + name + "\"")
                    .collect(Collectors.joining(", "));

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are a strategic business manager (Main Agent). Your goal is to maximize net worth.\n");
            promptBuilder.append("You issue commands to a Sub-Agent that executes them. You have advanced memory tools available.\n");
            promptBuilder.append("Your persona is: '").append(this.persona).append("'.\n");
            
            // Memory context
            String scratchpadContent = memoryManager.getScratchpad();
            if (scratchpadContent != null && !scratchpadContent.isEmpty()) {
                promptBuilder.append("\n# YOUR CURRENT NOTES (Scratchpad):\n").append(scratchpadContent).append("\n");
            }
            
            promptBuilder.append("\n# MEMORY TOOLS AVAILABLE:\n");
            promptBuilder.append("- write_to_memory: Store information in scratchpad, key-value store, or vector database\n");
            promptBuilder.append("- read_from_memory: Retrieve from scratchpad, search key-value store, or search vector database\n");
            promptBuilder.append("- Use vector database for long-term memory that survives context pruning\n");
            
            promptBuilder.append("\n# EXECUTION HISTORY:\n");
            for (Turn turn : history) {
                promptBuilder.append("Thought: ").append(turn.thought()).append("\n");
                promptBuilder.append("Command: ").append(turn.actionJson()).append("\n");
                promptBuilder.append("Result: '").append(turn.result()).append("'\n---\n");
            }

            promptBuilder.append("\n# CURRENT STATE:\n").append(stateJson).append("\n");
            promptBuilder.append("\n# AVAILABLE COMMANDS:\n[").append(toolList).append("]\n");

            promptBuilder.append("\n# STRATEGIC GUIDANCE:\n");
            promptBuilder.append("1. Use memory tools to maintain long-term business intelligence\n");
            promptBuilder.append("2. Store important patterns, supplier relationships, and performance data in vector database\n");
            promptBuilder.append("3. Use key-value store for structured data (supplier contacts, pricing strategies)\n");
            promptBuilder.append("4. Update scratchpad with current situation analysis\n");

            promptBuilder.append("\n# RESPONSE FORMAT:\n");
            promptBuilder.append("```json\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"thought\": \"<Your strategic reasoning including memory considerations>\",\n");
            promptBuilder.append("  \"action\": {\n");
            promptBuilder.append("    \"tool\": \"<command_name>\",\n");
            promptBuilder.append("    \"parameters\": { ... }\n");
            promptBuilder.append("  }\n");
            promptBuilder.append("}\n");
            promptBuilder.append("```");

            return promptBuilder.toString();
        } catch (Exception e) {
            return "Error: Could not perceive the environment: " + e.getMessage();
        }
    }

    public Action getActionFromHumanInstruction(String instruction, SimulationState state, Map<String, Tool> availableTools) {
        return new Action(SimulationEngine.IDLE_TOOL, "{}");
    }
}