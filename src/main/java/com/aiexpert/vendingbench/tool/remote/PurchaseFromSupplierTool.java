package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.util.ValidationUtils;
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
        StringBuilder errorBuilder = new StringBuilder();

        for (JsonNode itemNode : params.get("items")) {
            String name = itemNode.path("name").asText();
            int quantity = itemNode.path("quantity").asInt();

            try {
                ValidationUtils.validateItemName(name);
                ValidationUtils.validateQuantity(quantity);
            } catch (IllegalArgumentException e) {
                errorBuilder.append("Invalid order item '").append(name).append("': ").append(e.getMessage()).append("\n");
                continue;
            }

            // Check against the permanent product catalog, not the changing storage inventory.
            Item masterItem = state.getProductCatalog().get(name);
            if (masterItem == null) {
                errorBuilder.append("Item '").append(name).append("' is not a valid item and cannot be ordered.\n");
                continue;
            }

            double wholesaleCost = masterItem.getWholesaleCost();
            totalCost += wholesaleCost * quantity;
            itemsToOrder.put(name, quantity);
        }
        
        if (!errorBuilder.isEmpty()) {
            return "Error: Purchase failed due to invalid items.\n" + errorBuilder.toString().trim();
        }

        if (itemsToOrder.isEmpty()) {
            return "Error: No valid items were specified for purchase.";
        }

        if (totalCost > state.getCashBalance()) {
            return String.format("Error: Purchase failed. Order cost is $%.2f, but you only have $%.2f.", totalCost, state.getCashBalance());
        }

        state.setCashBalance(state.getCashBalance() - totalCost);
        int deliveryDays = 2 + random.nextInt(2);
        int arrivalDay = state.getDay() + deliveryDays;

        state.addPendingDelivery(arrivalDay, itemsToOrder);

        return String.format("Successfully purchased items for $%.2f. They will be delivered to your storage on Day %d.", totalCost, arrivalDay);
    }
}