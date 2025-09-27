package com.aiexpert.vendingbench.environment;

import com.aiexpert.vendingbench.llm.LLMService;
import com.aiexpert.vendingbench.llm.LLMServiceProvider;
import com.aiexpert.vendingbench.logging.EventLogger;
import com.aiexpert.vendingbench.model.Inventory;
import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SalesReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

@Service
public class CustomerSimulation {
    private final LLMServiceProvider llmServiceProvider;
    private final EventLogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public CustomerSimulation(LLMServiceProvider llmServiceProvider, EventLogger logger) {
        this.llmServiceProvider = llmServiceProvider;
        this.logger = logger;
    }

    public void initializeItemDemand(Collection<Item> items) {
        LLMService llmService = llmServiceProvider.getActiveService();
        for (Item item : items) {
            String prompt = String.format(
                "You are a market analyst. For a vending machine item '%s', provide a realistic price elasticity, a reference price in USD, and a base daily sales number. " +
                "Price elasticity should be a negative number, typically between -0.5 and -2.5. " +
                "Respond ONLY with a JSON object like: {\"elasticity\": -1.2, \"reference_price\": 1.50, \"base_sales\": 30}",
                item.getName()
            );
            logger.log("SIMULATION_SETUP", "CustomerSimulation", "Generating demand profile for " + item.getName());
            String response = llmService.generate(prompt);
            try {
                JsonNode demandNode = objectMapper.readTree(response);
                item.setElasticity(demandNode.path("elasticity").asDouble(-1.0));
                item.setReferencePrice(demandNode.path("reference_price").asDouble(item.getPrice()));
                item.setBaseSales(demandNode.path("base_sales").asInt(20));
                 logger.log("SIMULATION_SETUP", "CustomerSimulation", "Demand profile for " + item.getName() + " set.", Map.of("profile", response));
            } catch (Exception e) {
                logger.log("ERROR", "CustomerSimulation", "Failed to parse demand profile for " + item.getName() + ". Using defaults.", Map.of("error", e.getMessage()));
                item.setElasticity(-1.0);
                item.setReferencePrice(1.50);
                item.setBaseSales(20);
            }
        }
    }

    public SalesReport simulateDailySales(Inventory vendingMachineInventory, int day) {
        StringBuilder reportDetails = new StringBuilder("Daily Sales Report:\n");
        int dailyTotalUnitsSold = 0;
        double dailyTotalRevenue = 0.0;

        for (Item item : vendingMachineInventory.getClonedItems().values()) {
            if (item.getQuantity() > 0 && item.getPrice() > 0) {
                int potentialSales = calculateSales(item, day);
                int actualSales = Math.min(potentialSales, item.getQuantity());

                if (actualSales > 0) {
                    double revenue = actualSales * item.getPrice();
                    dailyTotalUnitsSold += actualSales;
                    dailyTotalRevenue += revenue;
                    reportDetails.append(String.format("- Sold %d units of %s for $%.2f.\n", actualSales, item.getName(), revenue));
                }
            }
        }

        String summary = String.format("A total of %d units were sold today.", dailyTotalUnitsSold);
        logger.log("CUSTOMER_SALES", "CustomerSimulation", summary, Map.of("details", reportDetails.toString().trim()));

        return new SalesReport(dailyTotalUnitsSold, dailyTotalRevenue, reportDetails.toString());
    }
    
    public int calculateSales(Item item, int day) {
        if (item.getReferencePrice() <= 0) return 0;
        double priceRatio = item.getPrice() / item.getReferencePrice();
        double priceEffect = Math.pow(priceRatio, item.getElasticity());
        DayOfWeek dayOfWeek = LocalDate.ofEpochDay(day).getDayOfWeek();
        double dayMultiplier = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) ? 1.5 : 1.0;
        double noise = 0.8 + (1.2 - 0.8) * random.nextDouble();
        double calculatedSales = item.getBaseSales() * priceEffect * dayMultiplier * noise;
        return (int) Math.round(Math.max(0, calculatedSales));
    }
}