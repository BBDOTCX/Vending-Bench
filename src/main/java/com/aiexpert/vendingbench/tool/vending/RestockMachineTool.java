package com.aiexpert.vendingbench.tool.vending;

import com.aiexpert.vendingbench.config.SimulationConfig;
import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.util.ValidationUtils;
import com.fasterxml.jackson.databind.JsonNode;

public class RestockMachineTool implements Tool {
    private final SimulationConfig config;

    public RestockMachineTool(SimulationConfig config) {
        this.config = config;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        if (params == null || !params.has("items") || !params.get("items").isArray()) {
            return "Error: 'items' array parameter is required for restock_machine.";
        }

        JsonNode itemsNode = params.get("items");
        StringBuilder resultBuilder = new StringBuilder();
        int itemsRestocked = 0;
        
        int currentVendingTotal = state.getVendingMachine().getItems().values()
            .stream()
            .mapToInt(Item::getQuantity)
            .sum();

        for (JsonNode itemNode : itemsNode) {
            if (!itemNode.has("name") || !itemNode.has("quantity")) {
                continue;
            }
            String itemName = itemNode.get("name").asText();
            int quantity = itemNode.get("quantity").asInt();

            try {
                ValidationUtils.validateItemName(itemName);
                ValidationUtils.validateQuantity(quantity);
            } catch (IllegalArgumentException e) {
                resultBuilder.append("Skipped ").append(itemName).append(": ").append(e.getMessage()).append(".\n");
                continue;
            }

            if (quantity <= 0) {
                resultBuilder.append("Skipped ").append(itemName).append(" (quantity not positive).\n");
                continue;
            }

            if (currentVendingTotal + quantity > config.getMaxVendingMachineCapacity()) {
                resultBuilder.append("Skipped ").append(itemName).append(": Would exceed vending machine capacity.\n");
                continue;
            }

            Item storageItem = state.getStorage().getItems().get(itemName);
            if (storageItem == null || storageItem.getQuantity() == 0) {
                resultBuilder.append("Failed to restock ").append(itemName).append(": Not available in storage.\n");
                continue;
            }

            int amountToMove = Math.min(quantity, storageItem.getQuantity());

            // Create a new item for the vending machine
            Item newItem = new Item(
                storageItem.getName(),
                amountToMove,
                storageItem.getPrice(),
                storageItem.getWholesaleCost()
            );
            
            // Manually copy the economic profile from the storage item to the new item
            newItem.setElasticity(storageItem.getElasticity());
            newItem.setBaseSales(storageItem.getBaseSales());
            newItem.setReferencePrice(storageItem.getReferencePrice());

            // Remove from storage and add the complete new item to the machine
            state.getStorage().removeItem(itemName, amountToMove);
            state.getVendingMachine().addItem(newItem);

            itemsRestocked++;
            currentVendingTotal += amountToMove;
            resultBuilder.append("Successfully moved ").append(amountToMove).append(" of ").append(itemName).append(" to vending machine.\n");
        }

        if (itemsRestocked == 0) {
            return "Error: No valid items were provided to restock.";
        }

        return resultBuilder.toString().trim();
    }
}