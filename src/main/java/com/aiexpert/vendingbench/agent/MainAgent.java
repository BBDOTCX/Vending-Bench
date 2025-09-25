package com.aiexpert.vendingbench.agent;

import com.aiexpert.vendingbench.llm.LLMService;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.service.SimulationEngine;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.stream.Collectors;

public class MainAgent extends Agent {

    private final LLMService llmService;
    private final EventLogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private String persona;
    private String lastActionResult;
    private String lastThought = "Waiting for first turn.";
    private Action humanOverrideAction = null;

    public MainAgent(String name, LLMService llmService, EventLogger logger) {
        super(name);
        this.llmService = llmService;
        this.logger = logger;
        this.persona = "A meticulous and profit-oriented business manager focused on long-term growth.";
    }

    public void setHumanOverrideAction(Action action) { this.humanOverrideAction = action; }
    public void setPersona(String persona) { this.persona = persona; }
    public void setLastActionResult(String lastActionResult) { this.lastActionResult = lastActionResult; }
    public String getLastThought() { return lastThought; }


    @Override
    public Action act(SimulationState state, Map<String, Tool> availableTools) {
        if (humanOverrideAction != null) {
            Action actionToExecute = humanOverrideAction;
            this.lastThought = "Executing user-provided action: " + actionToExecute.toolName();
            this.status = "Executing human input";
            this.humanOverrideAction = null;
            return actionToExecute;
        }
        
        if (llmService == null) {
            this.status = "Idle (No LLM)";
            this.lastThought = "LLM Service is not configured. Cannot act.";
            return new Action(SimulationEngine.IDLE_TOOL, "{}");
        }

        String perception = perceive(state, availableTools, null);
        logger.log("LLM_REQUEST", name, "Sending prompt to LLM.", Map.of("prompt", perception));

        this.status = "Thinking...";
        String responseJson = llmService.generate(perception);
        logger.log("LLM_RESPONSE", name, "Received response from LLM.", Map.of("response", responseJson));

        try {
            JsonNode responseNode = objectMapper.readTree(responseJson);
            this.lastThought = responseNode.has("thought") ? responseNode.get("thought").asText() : "No thought provided.";
            JsonNode actionNode = responseNode.get("action");
            if (actionNode == null) throw new IllegalArgumentException("Response missing 'action' object.");
            String toolName = actionNode.get("tool").asText();
            JsonNode paramsNode = actionNode.get("parameters");
            String paramsJson = (paramsNode != null) ? objectMapper.writeValueAsString(paramsNode) : "{}";
            if (availableTools.containsKey(toolName)) {
                this.status = "Executing tool: " + toolName;
                return new Action(toolName, paramsJson);
            } else {
                this.status = "Idle (invalid tool)";
                String error = "Error: LLM chose an invalid tool '" + toolName + "'.";
                setLastActionResult(error);
                return new Action(SimulationEngine.IDLE_TOOL, "{}");
            }
        } catch (Exception e) {
            this.status = "Idle (parsing error)";
            this.lastThought = "Error: Could not parse the LLM's response. It may be malformed JSON. Response: " + responseJson;
            System.err.println("Error parsing LLM response: " + e.getMessage());
            String error = "Error: Could not parse LLM response. Please ensure it is valid JSON with 'thought' and 'action' keys.";
            setLastActionResult(error);
            return new Action(SimulationEngine.IDLE_TOOL, "{}");
        }
    }

    public Action getActionFromHumanInstruction(String instruction, SimulationState state, Map<String, Tool> availableTools) {
        String translationPrompt = perceive(state, availableTools, instruction);
        logger.log("LLM_REQUEST", "HumanInputTranslator", "Sending translation prompt to LLM.", Map.of("prompt", translationPrompt));
        
        String responseJson = llmService.generate(translationPrompt);
        logger.log("LLM_RESPONSE", "HumanInputTranslator", "Received translation from LLM.", Map.of("response", responseJson));

        try {
            JsonNode responseNode = objectMapper.readTree(responseJson);
            JsonNode actionNode = responseNode.get("action");
            String toolName = actionNode.get("tool").asText();
            JsonNode paramsNode = actionNode.get("parameters");
            String paramsJson = (paramsNode != null) ? objectMapper.writeValueAsString(paramsNode) : "{}";
            return new Action(toolName, paramsJson);
        } catch (Exception e) {
            System.err.println("Could not translate human input to action: " + e.getMessage());
            return new Action(SimulationEngine.IDLE_TOOL, "{}");
        }
    }

    private String perceive(SimulationState state, Map<String, Tool> availableTools, String humanInstruction) {
        try {
            String stateJson = objectMapper.writeValueAsString(state);
            String toolList = availableTools.keySet().stream()
                    .map(name -> "\"" + name + "\"")
                    .collect(Collectors.joining(", "));

            StringBuilder promptBuilder = new StringBuilder();

            if (humanInstruction != null) {
                promptBuilder.append("You are a helpful AI assistant. Your task is to translate a human's natural language instruction into a single, specific JSON tool call.\n");
                promptBuilder.append("\n# Human's Instruction:\n").append(humanInstruction).append("\n");
                promptBuilder.append("\n# Current State:\n").append(stateJson).append("\n");
                promptBuilder.append("\n# Available Tools:\n[").append(toolList).append("]\n");
                promptBuilder.append("\n# Task:\n");
                promptBuilder.append("Based on the human's instruction and the current state, what is the single most appropriate JSON tool call to execute? Respond ONLY with the JSON object, formatted exactly like the 'Response Format' example below. Do not include the 'thought' field for this translation task.");

            } else {
                promptBuilder.append("You are an autonomous agent managing a vending machine business.\n");
                promptBuilder.append("Your persona is: '").append(this.persona).append("'.\n");
                promptBuilder.append("Your goal is to maximize net worth over the long term.\n");

                if (lastActionResult != null && !lastActionResult.isBlank()) {
                    promptBuilder.append("\n# Previous Action Result:\n");
                    promptBuilder.append("The result of your last action was: '").append(lastActionResult).append("'.\n");
                    promptBuilder.append("Analyze this result. If it was an error, identify the cause and correct your next action.\n");
                }

                promptBuilder.append("\n# Current State:\n").append(stateJson).append("\n");
                promptBuilder.append("\n# Available Tools:\n[").append(toolList).append("]\n");

                promptBuilder.append("\n# Task:\n");
                promptBuilder.append("Based on the current state and previous action result, decide on the single best action to take next.\n");
                promptBuilder.append("1. First, think step-by-step about the current situation, your goals, and what the best course of action is. Explain your reasoning in a 'thought' field.\n");
                promptBuilder.append("2. Second, specify the single tool call to execute in an 'action' field.\n");
                promptBuilder.append("3. IMPORTANT: To send an email, use the 'compose_email_to_contact' tool. Provide a 'contact_type' (supplier, maintenance, etc.) and a 'topic'. Do NOT use this for placing orders; use 'purchase_from_supplier' for that.\n");
                promptBuilder.append("4. If your storage is empty, you MUST use 'purchase_from_supplier' to order more items.\n");
                promptBuilder.append("5. If you have ordered items but they have not arrived, you MUST use 'wait_for_next_day' to advance time.\n");
                promptBuilder.append("6. If you are stuck or unsure, use the 'ask_for_human_help' tool.\n");
            }

            promptBuilder.append("\n# Response Format:\n");
            promptBuilder.append("Respond ONLY with a single, valid JSON object. It must contain an 'action' object. If you are thinking for yourself (not translating), it must also contain a 'thought' field, like this:\n");
            promptBuilder.append("```json\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"thought\": \"My storage is empty. I need to order more supplies. I'll email the supplier to ask for a product catalog first.\",\n");
            promptBuilder.append("  \"action\": {\n");
            promptBuilder.append("    \"tool\": \"compose_email_to_contact\",\n");
            promptBuilder.append("    \"parameters\": {\n");
            promptBuilder.append("      \"contact_type\": \"supplier\",\n");
            promptBuilder.append("      \"topic\": \"Request for current product catalog and pricing\"\n");
            promptBuilder.append("    }\n");
            promptBuilder.append("  }\n");
            promptBuilder.append("}\n");
            promptBuilder.append("```");

            return promptBuilder.toString();
        } catch (Exception e) {
            return "Error: Could not perceive the environment: " + e.getMessage();
        }
    }
}