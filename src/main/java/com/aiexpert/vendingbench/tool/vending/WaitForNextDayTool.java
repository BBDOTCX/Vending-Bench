 package com.aiexpert.vendingbench.tool.vending;

import com.aiexpert.vendingbench.environment.CustomerSimulation;
import com.aiexpert.vendingbench.model.Inventory;
import com.aiexpert.vendingbench.model.SalesReport;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class WaitForNextDayTool implements Tool {
    private final CustomerSimulation customerSimulation;

    public WaitForNextDayTool(CustomerSimulation customerSimulation) {
        this.customerSimulation = customerSimulation;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        // Step 1: Simulate the sales for the current day based on the machine's inventory.
        Inventory machine = state.getVendingMachine();
        SalesReport report = customerSimulation.simulateDailySales(machine, state.getDay());

        // Step 2: Apply the sales results by removing sold items from the vending machine.
        // We iterate over a clone of the items to safely modify the original map while looping.
        machine.getClonedItems().forEach((itemName, item) -> {
            if (item.getQuantity() > 0 && item.getPrice() > 0) {
                int potentialSales = customerSimulation.calculateSales(item, state.getDay());
                int actualSales = Math.min(potentialSales, item.getQuantity());
                if (actualSales > 0) {
                    machine.removeItem(itemName, actualSales);
                }
            }
        });

        // Step 3: Add the generated revenue to the machine's cash box and update total sales.
        machine.addCash(report.revenue());
        state.setTotalUnitsSold(state.getTotalUnitsSold() + report.unitsSold());

        // Step 4: Advance the simulation to the next day and apply the daily fee.
        state.incrementDay();

        // Step 5: Return a comprehensive result string for the agent.
        String result = String.format(
            "Advanced to Day %d. Today's sales: %d units sold for a total of $%.2f. Daily fee of $%.2f deducted.",
            state.getDay(),
            report.unitsSold(),
            report.revenue(),
            state.getDailyFee()
        );

        return result;
    }
}