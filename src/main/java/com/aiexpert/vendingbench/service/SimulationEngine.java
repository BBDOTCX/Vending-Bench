package com.aiexpert.vendingbench.service;

import com.aiexpert.vendingbench.agent.Action;
import com.aiexpert.vendingbench.agent.MainAgent;
import com.aiexpert.vendingbench.agent.SubAgent;
import com.aiexpert.vendingbench.config.SimulationConfig;
import com.aiexpert.vendingbench.environment.CustomerSimulation;
import com.aiexpert.vendingbench.environment.EmailSimulation;
import com.aiexpert.vendingbench.llm.LLMServiceProvider;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.tool.memory.ReadFromMemoryTool;
import com.aiexpert.vendingbench.tool.memory.WriteToMemoryTool;
import com.aiexpert.vendingbench.tool.remote.*;
import com.aiexpert.vendingbench.tool.vending.*;
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

    private final LLMServiceProvider llmServiceProvider;
    private final SimulationConfig config;
    private final CustomerSimulation customerSimulation;
    private final EmailSimulation emailSimulation;
    private final EventLogger logger;
    private final TokenizerService tokenizerService;
    private final SubAgent subAgent;
    private final MemoryManager memoryManager;
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

    public SimulationEngine(LLMServiceProvider llmServiceProvider, SimulationConfig config, CustomerSimulation cs, EmailSimulation es, EventLogger logger, TokenizerService ts, SubAgent subAgent, MemoryManager memoryManager) {
        this.llmServiceProvider = llmServiceProvider;
        this.config = config;
        this.customerSimulation = cs;
        this.emailSimulation = es;
        this.logger = logger;
        this.tokenizerService = ts;
        this.subAgent = subAgent;
        this.memoryManager = memoryManager;
        initializeTools();
        reset();
    }

    private void initializeTools() {
        this.availableTools = new HashMap<>();
        availableTools.put("set_prices", new SetPricesTool(config));
        availableTools.put("restock_machine", new RestockMachineTool(config));
        availableTools.put("read_email", new ReadEmailTool());
        availableTools.put("collect_cash", new CollectCashTool());
        availableTools.put("get_money_balance", new GetMoneyBalanceTool());
        availableTools.put("view_inventory", new ViewInventoryTool());
        availableTools.put("internet_search", new InternetSearchTool());
        availableTools.put("purchase_from_supplier", new PurchaseFromSupplierTool());
        availableTools.put("view_email_history", new ViewEmailHistoryTool());
        availableTools.put(IDLE_TOOL, (params, state) -> "Agent is idle, thinking about its next move.");
        availableTools.put(HUMAN_HELP_TOOL, (params, state) -> "Agent has requested human intervention. The simulation is paused.");

        // Memory tools with vector database support
        availableTools.put("write_to_memory", new WriteToMemoryTool(memoryManager));
        availableTools.put("read_from_memory", new ReadFromMemoryTool(memoryManager));
    }

    public void start(String provider, String apiKey, String modelName, String persona, int maxTurns, boolean verboseLogging, boolean humanHelpTimeout, boolean disableHumanHelp) {
        if (running.get()) return;
        reset();
        status = "Initializing...";

        this.verboseLoggingEnabled = verboseLogging;
        this.humanHelpTimeoutEnabled = humanHelpTimeout;

        llmServiceProvider.setActiveService(provider, apiKey, modelName);
        // Configure embeddings with the same API key
        memoryManager.configureEmbeddings(apiKey);

        this.mainAgent = new MainAgent("MainAgent", llmServiceProvider.getActiveService(), logger, tokenizerService, memoryManager);
        
        availableTools.put("compose_email_to_contact", new ComposeEmailToContactTool(emailSimulation, llmServiceProvider));
        availableTools.put("wait_for_next_day", new WaitForNextDayTool(customerSimulation));

        if (disableHumanHelp) {
            availableTools.remove(HUMAN_HELP_TOOL);
        }

        mainAgent.setPersona(persona);
        this.maxTurns = maxTurns > 0 ? maxTurns : config.getMaxTurns();
        
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
            while (running.get() && state.getTurn() <= maxTurns) {
                while (paused.get()) {
                    Thread.sleep(100);
                }

                if (state.getDay() > lastDayProcessed) {
                    processDeliveries();
                    lastDayProcessed = state.getDay();
                }

                logger.log("TURN_START", "SimulationEngine", "--- Turn " + state.getTurn() + " (Day " + state.getDay() + ") ---");
                
                Action plannedAction = mainAgent.act(state, availableTools);
                logger.log("TOOL_CALL", mainAgent.getName(), "Planned action: " + plannedAction.toolName());

                if (HUMAN_HELP_TOOL.equals(plannedAction.toolName())) {
                    status = "Awaiting Human Input";
                    paused.set(true);
                    helpRequestTimestamp = System.currentTimeMillis();
                    logger.log("HUMAN_INTERVENTION", mainAgent.getName(), "Agent requested help. Pausing simulation.");
                    continue;
                }
                
                String result = subAgent.execute(plannedAction, state, availableTools);
                logger.log("TOOL_RESULT", subAgent.getName(), "Execution finished.", Map.of("result", result));

                mainAgent.updateHistory(result);

                state.incrementTurn();
                Thread.sleep(config.getTurnDelayMs());
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
                running.set(false);
            }
        }
    }
    
    private void processDeliveries() {
    Map<String, Integer> todaysDeliveries = state.getPendingDeliveries().remove(state.getDay());
    if (todaysDeliveries != null && !todaysDeliveries.isEmpty()) {
        StringBuilder deliveryReport = new StringBuilder();
        todaysDeliveries.forEach((itemName, quantity) -> {
            // Check against product catalog instead of current storage
            Item catalogItem = state.getProductCatalog().get(itemName);
            if (catalogItem != null) {
                state.getStorage().addOrUpdateItem(itemName, quantity, catalogItem.getPrice(), catalogItem.getWholesaleCost());
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
        this.state = new SimulationState(config.getInitialCashBalance(), config.getDailyFee()); 
        this.logger.clear(); 
        this.memoryManager.reset();
        this.status = "Idle"; 
        this.paused.set(false); 

        if (mainAgent == null) {
            mainAgent = new MainAgent("MainAgent", null, logger, tokenizerService, memoryManager);
        }
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
}