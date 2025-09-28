package com.aiexpert.vendingbench.service;

import com.aiexpert.vendingbench.agent.Action;
import com.aiexpert.vendingbench.agent.MainAgent;
import com.aiexpert.vendingbench.agent.SubAgent;
import com.aiexpert.vendingbench.config.SimulationDefaults;
import com.aiexpert.vendingbench.environment.CustomerSimulation;
import com.aiexpert.vendingbench.llm.LLMServiceFactory;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.util.StateCloner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SimulationEngine {

    private static final Logger verboseJsonLogger = LoggerFactory.getLogger("VerboseJsonLogger");

    public static final String IDLE_TOOL = "idle";
    public static final String HUMAN_HELP_TOOL = "ask_for_human_help";
    private static final long HUMAN_HELP_TIMEOUT_MS = 30000;

    private final LLMServiceFactory llmServiceFactory;
    private final SimulationDefaults defaults;
    private final CustomerSimulation customerSimulation;
    private final EventLogger logger;
    private final TokenizerService tokenizerService;
    private final SubAgent subAgent;
    private final MemoryManager memoryManager;
    private final ToolService toolService;
    private final SafetyService safetyService;
    private final StateCloner stateCloner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SimulationState state;
    private MainAgent mainAgent;
    private Map<String, Tool> availableTools;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private int maxTurns;
    private String status = "Idle";
    private boolean verboseLoggingEnabled = true;
    private boolean humanHelpTimeoutEnabled = false;
    private long helpRequestTimestamp = 0;

    public SimulationEngine(LLMServiceFactory llmServiceFactory, SimulationDefaults defaults, CustomerSimulation cs, EventLogger logger, TokenizerService ts, SubAgent subAgent, MemoryManager memoryManager, ToolService toolService, SafetyService safetyService, StateCloner stateCloner) {
        this.llmServiceFactory = llmServiceFactory;
        this.defaults = defaults;
        this.customerSimulation = cs;
        this.logger = logger;
        this.tokenizerService = ts;
        this.subAgent = subAgent;
        this.memoryManager = memoryManager;
        this.toolService = toolService;
        this.safetyService = safetyService;
        this.stateCloner = stateCloner;
        reset();
    }

    public void start(String provider, String apiKey, String modelName, String persona, int maxTurns, boolean verboseLogging, boolean humanHelpTimeout, boolean disableHumanHelp) {
        if (running.get()) return;
        reset();
        status = "Initializing...";

        this.verboseLoggingEnabled = verboseLogging;
        this.humanHelpTimeoutEnabled = humanHelpTimeout;

        llmServiceFactory.configureActiveService(provider, apiKey, modelName);
        this.availableTools = toolService.getAvailableTools(disableHumanHelp);

        this.mainAgent = new MainAgent("MainAgent", llmServiceFactory.getActiveService(), logger, tokenizerService, memoryManager);
        mainAgent.setPersona(persona);
        this.maxTurns = maxTurns > 0 ? maxTurns : defaults.getSimulation().getMaxTurns();
        
        logger.log("SIMULATION_SETUP", "SimulationEngine", "Initializing item demand profiles...");
        customerSimulation.initializeItemDemand(state.getStorage().getItems().values());
        
        running.set(true);
        paused.set(false);
        executor.submit(this::runSimulationLoop);
    }
    
    public void addTurns(int turnsToAdd) {
        if (!status.equals("Finished") || turnsToAdd <= 0) {
            return;
        }
        logger.log("SIMULATION_UPDATE", "Human", "Adding " + turnsToAdd + " more turns to the simulation.");
        this.maxTurns += turnsToAdd;
        running.set(true);
        paused.set(false);
        status = "Running";
        executor.submit(this::runSimulationLoop);
    }

    private void logVerbose(String type, String source, Map<String, Object> details) {
        if (!verboseLoggingEnabled) return;
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("timestamp", Instant.now().toString());
        logMap.put("type", type);
        logMap.put("source", source);
        logMap.put("details", details);
        try {
            verboseJsonLogger.info(objectMapper.writeValueAsString(logMap));
        } catch (JsonProcessingException e) {
            verboseJsonLogger.info("{\"error\": \"Failed to serialize log entry\"}");
        }
    }

    public void resumeWithHumanInput(String humanPrompt) {
        if (status.equals("Awaiting Human Input") || status.equals("Paused")) {
            logger.log("HUMAN_INTERVENTION", "Human", "Providing help: " + humanPrompt);
            Action humanAction = mainAgent.getActionFromHumanInstruction(humanPrompt, state, availableTools);
            mainAgent.setHumanOverrideAction(humanAction);
            helpRequestTimestamp = 0; 
            paused.set(false);
            status = "Running";
        }
    }

    private void runSimulationLoop() {
        logger.log("SIMULATION_START", "SimulationEngine", "Simulation loop started.");
        status = "Running";

        try {
            int lastDayProcessed = state.getDay() - 1;
            boolean isHumanHelpDisabled = !availableTools.containsKey(HUMAN_HELP_TOOL);

            while (running.get() && state.getTurn() <= maxTurns) {
                while (paused.get()) {
                    if (humanHelpTimeoutEnabled && helpRequestTimestamp > 0 &&
                        (System.currentTimeMillis() - helpRequestTimestamp > HUMAN_HELP_TIMEOUT_MS)) {
                        
                        logger.log("HUMAN_INTERVENTION", "SimulationEngine", "Human help timeout reached. Resuming simulation automatically.");
                        mainAgent.setHumanOverrideAction(new Action(IDLE_TOOL, "{}"));
                        helpRequestTimestamp = 0;
                        paused.set(false);
                        status = "Running";
                        break;
                    }
                    Thread.sleep(100);
                }

                if (state.getDay() > lastDayProcessed) {
                    processDeliveries();
                    lastDayProcessed = state.getDay();
                }

                Action plannedAction = mainAgent.act(stateCloner.clone(state), availableTools);
                logger.log("TOOL_CALL", mainAgent.getName(), "Planned action: " + plannedAction.toolName());

                safetyService.recordAction(plannedAction);
                if (safetyService.isMeltdownDetected()) {
                    status = "Error: Meltdown detected. Agent is stuck in a loop. Halting simulation.";
                    logger.log("ERROR", "SafetyService", status);
                    running.set(false);
                    continue;
                }

                if (HUMAN_HELP_TOOL.equals(plannedAction.toolName())) {
                    if (isHumanHelpDisabled) {
                        logger.log("ERROR", mainAgent.getName(), "Agent attempted to call 'ask_for_human_help' but it was disabled. Forcing idle action.");
                        plannedAction = new Action(IDLE_TOOL, "{}");
                    } else {
                        status = "Awaiting Human Input";
                        paused.set(true);
                        helpRequestTimestamp = System.currentTimeMillis();
                        logger.log("HUMAN_INTERVENTION", mainAgent.getName(), "Agent requested help. Pausing simulation.");
                        continue;
                    }
                }
                
                String result = subAgent.execute(plannedAction, state, availableTools);
                logger.log("TOOL_RESULT", subAgent.getName(), "Execution finished.", Map.of("result", result));

                mainAgent.updateHistory(result);

                Map<String, Object> turnDetails = new HashMap<>();
                turnDetails.put("turn", state.getTurn());
                turnDetails.put("day", state.getDay());
                turnDetails.put("thought", mainAgent.getLastThought());
                turnDetails.put("action", plannedAction);
                turnDetails.put("result", result);
                turnDetails.put("state", state);
                logVerbose("TURN_DATA", "SimulationEngine", turnDetails);

                state.incrementTurn();
                Thread.sleep(defaults.getSimulation().getTurnDelayMs());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = "Error: Simulation interrupted.";
        } catch (Exception e) {
            e.printStackTrace();
            status = "Error: " + e.getMessage();
        } finally {
            if (state.getTurn() > maxTurns) {
                status = "Finished";
            }
            if (!status.startsWith("Error")) {
                running.set(false);
            }
        }
    }
    
    private void processDeliveries() {
        Map<String, Integer> todaysDeliveries = state.getPendingDeliveries().remove(state.getDay());
        if (todaysDeliveries != null && !todaysDeliveries.isEmpty()) {
            StringBuilder deliveryReport = new StringBuilder();
            todaysDeliveries.forEach((itemName, quantity) -> {
                Item masterItem = state.getStorage().getItem(itemName);
                if (masterItem == null) {
                    masterItem = state.getProductCatalog().get(itemName);
                }

                if (masterItem != null) {
                    state.getStorage().addOrUpdateItem(itemName, quantity, masterItem.getPrice(), masterItem.getWholesaleCost());
                    deliveryReport.append(String.format("%d units of %s, ", quantity, itemName));
                } else {
                    logger.log("ERROR", "SimulationEngine", "Cannot deliver unknown item: " + itemName);
                }
            });
            if (deliveryReport.length() > 0) {
                 String reportStr = "Delivery arrived: " + deliveryReport.substring(0, deliveryReport.length() - 2) + ".";
                 logger.log("DELIVERY_RECEIVED", "SimulationEngine", reportStr);
            }
        }
    }

    public void togglePause() { 
        if(running.get() && !status.equals("Awaiting Human Input")){ 
            paused.set(!paused.get()); 
            status = paused.get() ? "Paused" : "Running"; 
        } 
    }

    public void reset() { 
        running.set(false);
        if (executor != null && !executor.isTerminated()) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        executor = Executors.newSingleThreadExecutor(); 
        this.state = new SimulationState(defaults.getSimulation().getInitialCashBalance(), defaults.getSimulation().getDailyFee()); 
        this.logger.clear(); 
        this.memoryManager.reset();
        this.safetyService.reset();
        this.status = "Idle"; 
        this.paused.set(false); 

        if (mainAgent == null) {
            mainAgent = new MainAgent("MainAgent", null, logger, tokenizerService, memoryManager);
        }
    }

    public double calculateNetWorth() { 
        if (state == null) return defaults.getSimulation().getInitialCashBalance();
        double inventoryValue = state.getStorage().getItems().values().stream()
                .mapToDouble(item -> item.getQuantity() * item.getWholesaleCost()).sum();
        inventoryValue += state.getVendingMachine().getItems().values().stream()
                .mapToDouble(item -> item.getQuantity() * item.getWholesaleCost()).sum();
        return state.getCashBalance() + state.getVendingMachine().getCashHeld() + inventoryValue; 
    }

    // --- Getters for Controller ---
    public String getStatus() { return status; }
    public MainAgent getMainAgent() { return mainAgent; }
    public SubAgent getSubAgent() { return subAgent; }
    public SimulationState getCurrentState() { return state; }
    public List<String> getMainEventLog() { return logger.getMainLog(); }
}