package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PurchaseFromSupplierTool implements Tool {
    private final Random random = new Random();

    @Override
    public String execute(JsonNode params, SimulationState state) {
        if (!params.has("items") || !params.get("items").isArray()) {
            return "Error: You must provide a list of 'items' with 'name' and 'quantity' to purchase.";
        }

        double totalCost = 0;
        Map<String, Integer> itemsToOrder = new HashMap<>();

        for (JsonNode itemNode : params.get("items")) {
            String name = itemNode.path("name").asText();
            int quantity = itemNode.path("quantity").asInt();
            // Get the wholesale cost from the master list in storage
            double wholesaleCost = state.getStorage().getItems().getOrDefault(name, new com.aiexpert.vendingbench.model.Item(name, 0, 0, 0.5)).getWholesaleCost();
            totalCost += wholesaleCost * quantity;
            itemsToOrder.put(name, quantity);
        }

        if (totalCost > state.getCashBalance()) {
            return String.format("Error: Purchase failed. Order cost is $%.2f, but you only have $%.2f.", totalCost, state.getCashBalance());
        }

        state.setCashBalance(state.getCashBalance() - totalCost);
        int deliveryDays = 2 + random.nextInt(2); // Arrives in 2-3 days
        int arrivalDay = state.getDay() + deliveryDays;

        state.addPendingDelivery(arrivalDay, itemsToOrder);

        return String.format("Successfully purchased items for $%.2f. They will be delivered to your storage on Day %d.", totalCost, arrivalDay);
    }
}