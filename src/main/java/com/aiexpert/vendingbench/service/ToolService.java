package com.aiexpert.vendingbench.service;

import com.aiexpert.vendingbench.config.SimulationDefaults;
import com.aiexpert.vendingbench.environment.CustomerSimulation;
import com.aiexpert.vendingbench.environment.EmailSimulation;
import com.aiexpert.vendingbench.llm.LLMServiceFactory;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.tool.memory.ReadFromMemoryTool;
import com.aiexpert.vendingbench.tool.memory.WriteToMemoryTool;
import com.aiexpert.vendingbench.tool.remote.*;
import com.aiexpert.vendingbench.tool.vending.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ToolService {

    private final SimulationDefaults defaults;
    private final MemoryManager memoryManager;
    private final CustomerSimulation customerSimulation;
    private final EmailSimulation emailSimulation;
    private final LLMServiceFactory llmServiceFactory;

    public ToolService(SimulationDefaults defaults, MemoryManager memoryManager, CustomerSimulation customerSimulation, EmailSimulation emailSimulation, LLMServiceFactory llmServiceFactory) {
        this.defaults = defaults;
        this.memoryManager = memoryManager;
        this.customerSimulation = customerSimulation;
        this.emailSimulation = emailSimulation;
        this.llmServiceFactory = llmServiceFactory;
    }

    public Map<String, Tool> getAvailableTools(boolean disableHumanHelp) {
        Map<String, Tool> tools = new HashMap<>();
        
        // Vending Tools
        tools.put("set_prices", new SetPricesTool(defaults));
        tools.put("restock_machine", new RestockMachineTool(defaults));
        tools.put("collect_cash", new CollectCashTool());
        tools.put("view_inventory", new ViewInventoryTool());
        tools.put("wait_for_next_day", new WaitForNextDayTool(customerSimulation));

        // Remote Tools
        tools.put("get_money_balance", new GetMoneyBalanceTool());
        tools.put("internet_search", new InternetSearchTool());
        tools.put("purchase_from_supplier", new PurchaseFromSupplierTool());
        tools.put("read_email", new ReadEmailTool());
        tools.put("view_email_history", new ViewEmailHistoryTool());
        tools.put("compose_email_to_contact", new ComposeEmailToContactTool(emailSimulation, llmServiceFactory));
        
        // Memory Tools
        tools.put("write_to_memory", new WriteToMemoryTool(memoryManager));
        tools.put("read_from_memory", new ReadFromMemoryTool(memoryManager));
        
        // System Tools
        tools.put(SimulationEngine.IDLE_TOOL, (params, state) -> "Agent is idle, thinking about its next move.");
        if (!disableHumanHelp) {
            tools.put(SimulationEngine.HUMAN_HELP_TOOL, (params, state) -> "Agent has requested human intervention. The simulation is paused.");
        }

        return tools;
    }
}