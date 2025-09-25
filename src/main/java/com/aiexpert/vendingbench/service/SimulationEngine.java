package com.aiexpert.vendingbench.service;

import com.aiexpert.vendingbench.agent.Action;
import com.aiexpert.vendingbench.agent.MainAgent;
import com.aiexpert.vendingbench.agent.SubAgent;
import com.aiexpert.vendingbench.config.SimulationConfig;
import com.aiexpert.vendingbench.environment.CustomerSimulation;
import com.aiexpert.vendingbench.environment.EmailSimulation;
import com.aiexpert.vendingbench.llm.LLMServiceProvider;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.logging.LogEntry;
import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.tool.remote.*;
import com.aiexpert.vendingbench.tool.vending.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SimulationEngine {

    public static final String IDLE_TOOL = "idle";
    public static final String HUMAN_HELP_TOOL = "ask_for_human_help";

    private final LLMServiceProvider llmServiceProvider;
    private final SimulationConfig config;
    private final CustomerSimulation customerSimulation;
    private final EmailSimulation emailSimulation;
    private final EventLogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SimulationState state;
    private MainAgent mainAgent;
    private SubAgent subAgent;
    private Map<String, Tool> availableTools;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private int maxTurns;
    private String status = "Idle";

    public SimulationEngine(LLMServiceProvider llmServiceProvider, SimulationConfig config, CustomerSimulation customerSimulation, EmailSimulation emailSimulation, EventLogger logger) {
        this.llmServiceProvider = llmServiceProvider;
        this.config = config;
        this.customerSimulation = customerSimulation;
        this.emailSimulation = emailSimulation;
        this.logger = logger;
        initializeTools();
        reset();
    }

    private void initializeTools() {
        this.availableTools = new HashMap<>();

        // Tools that now require the SimulationConfig
        availableTools.put("set_prices", new SetPricesTool(config));
        availableTools.put("restock_machine", new RestockMachineTool(config));

        // Tools that don't need injected dependencies
        availableTools.put("read_email", new ReadEmailTool());
        availableTools.put("collect_cash", new CollectCashTool());
        availableTools.put("get_money_balance", new GetMoneyBalanceTool());
        availableTools.put("view_inventory", new ViewInventoryTool());
        availableTools.put("internet_search", new InternetSearchTool());
        availableTools.put("purchase_from_supplier", new PurchaseFromSupplierTool());
        availableTools.put("view_email_history", new ViewEmailHistoryTool()); // New tool registered
        availableTools.put(IDLE_TOOL, (params, state) -> "Agent is idle, thinking about its next move.");
        availableTools.put(HUMAN_HELP_TOOL, (params, state) -> "Agent has requested human intervention. The simulation is paused.");
    }

    public void start(String provider, String apiKey, String modelName, String persona, int maxTurns) {
        if (running.get()) return;
        reset();
        status = "Initializing...";

        llmServiceProvider.setActiveService(provider, apiKey, modelName);

        // Re-create agents and tools that depend on the active LLM service
        this.mainAgent = new MainAgent("MainAgent", llmServiceProvider.getActiveService(), logger);
        this.subAgent = new SubAgent("SubAgent");
        
        // Tools that need injected dependencies are added here
        availableTools.put("send_email", new SendEmailTool(emailSimulation));
        availableTools.put("wait_for_next_day", new WaitForNextDayTool(customerSimulation));

        mainAgent.setPersona(persona);
        this.maxTurns = maxTurns > 0 ? maxTurns : config.getMaxTurns();
        
        logger.log("SIMULATION_SETUP", "SimulationEngine", "Initializing item demand profiles...");
        customerSimulation.initializeItemDemand(state.getStorage().getItems().values());
        
        running.set(true);
        paused.set(false);
        executor.submit(this::runSimulationLoop);
    }

    public void resumeWithHumanInput(String humanPrompt) {
        if (status.equals("Awaiting Human Input")) {
            logger.log("HUMAN_INTERVENTION", "Human", "Providing help: " + humanPrompt, null);
            Action humanAction = mainAgent.getActionFromHumanInstruction(humanPrompt, state, availableTools);
            mainAgent.setHumanOverrideAction(humanAction);
            paused.set(false);
            status = "Running";
        }
    }

    private void runSimulationLoop() {
        logger.log("SIMULATION_START", "SimulationEngine", "Simulation loop started.", Map.of("maxTurns", maxTurns));
        status = "Running";
        try {
            int lastDayProcessed = 0;
            while (running.get() && state.getTurn() <= maxTurns) {
                while (paused.get()) { Thread.sleep(100); }

                if (state.getDay() > lastDayProcessed) {
                    processDeliveries();
                    lastDayProcessed = state.getDay();
                }

                logger.log("TURN_START", "SimulationEngine", "--- Turn " + state.getTurn() + " (Day " + state.getDay() + ") ---", null);
                Action action = mainAgent.act(state, availableTools);

                if (HUMAN_HELP_TOOL.equals(action.toolName())) {
                    status = "Awaiting Human Input";
                    paused.set(true);
                    logger.log("HUMAN_INTERVENTION", mainAgent.getName(), "Agent requested help. Pausing simulation.", null);
                    continue; 
                }

                if (availableTools.containsKey(action.toolName())) {
                    logger.log("TOOL_CALL", mainAgent.getName(), "Executing tool: " + action.toolName(), Map.of("parameters", action.parameters()));
                    Tool tool = availableTools.get(action.toolName());
                    String result = tool.execute(objectMapper.readTree(action.parameters()), state);
                    mainAgent.setLastActionResult(result);
                    logger.log("TOOL_RESULT", mainAgent.getName(), "Tool execution finished.", Map.of("result", result));
                } else {
                     String errorMsg = "Agent attempted to use an unknown tool: " + action.toolName();
                     logger.log("ERROR", mainAgent.getName(), errorMsg, null);
                     mainAgent.setLastActionResult(errorMsg);
                }

                state.incrementTurn();
                Thread.sleep(config.getTurnDelayMs());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = "Error: Simulation interrupted.";
            logger.log("ERROR", "SimulationEngine", "Simulation loop was interrupted.");
        } catch (Exception e) {
            e.printStackTrace();
            status = "Error: " + e.getMessage();
            logger.log("CRITICAL_ERROR", "SimulationEngine", "A critical error stopped the simulation.", Map.of("error", e.getMessage()));
        } finally {
            status = "Finished";
            running.set(false);
        }
    }
    
    private void processDeliveries() {
        Map<String, Integer> todaysDeliveries = state.getPendingDeliveries().remove(state.getDay());
        if (todaysDeliveries != null && !todaysDeliveries.isEmpty()) {
            StringBuilder deliveryReport = new StringBuilder();
            todaysDeliveries.forEach((itemName, quantity) -> {
                // Added null safety check
                Item masterItem = state.getStorage().getItem(itemName);
                if (masterItem != null) {
                    state.getStorage().addOrUpdateItem(itemName, quantity, masterItem.getPrice(), masterItem.getWholesaleCost());
                    deliveryReport.append(String.format("%d units of %s, ", quantity, itemName));
                } else {
                    logger.log("ERROR", "SimulationEngine", "Cannot deliver unknown item: " + itemName, 
                              Map.of("itemName", itemName, "quantity", quantity));
                }
            });
            if (deliveryReport.length() > 0) {
                 String reportStr = "Delivery arrived: " + deliveryReport.substring(0, deliveryReport.length() - 2) + ".";
                 logger.log("DELIVERY_RECEIVED", "SimulationEngine", reportStr, null);
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
        
        this.state = new SimulationState(config.getInitialCashBalance(), config.getDailyFee()); 
        this.logger.clear(); 
        this.status = "Idle"; 
        this.paused.set(false); 
        
        // Create a default agent for the initial state
        if (mainAgent == null) {
            mainAgent = new MainAgent("MainAgent", null, logger);
        }
        this.mainAgent.setLastActionResult(null); 
    }

    public double calculateNetWorth() { 
        if (state == null) return config.getInitialCashBalance();
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
    public List<LogEntry> getVerboseLog() { return logger.getVerboseLog(); }
}