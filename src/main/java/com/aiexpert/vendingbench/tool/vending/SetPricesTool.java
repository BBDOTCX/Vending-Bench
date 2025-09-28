package com.aiexpert.vendingbench.tool.vending;

import com.aiexpert.vendingbench.config.SimulationConfig;
import com.aiexpert.vendingbench.model.Item;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.aiexpert.vendingbench.util.ValidationUtils;
import com.fasterxml.jackson.databind.JsonNode;

public class SetPricesTool implements Tool {
    private final SimulationConfig config;

    public SetPricesTool(SimulationConfig config) {
        this.config = config;
    }

    @Override
    public String execute(JsonNode params, SimulationState state) {
        if (!params.has("prices") || !params.get("prices").isArray()) {
            return "Error: 'prices' array parameter is required, with each element having a 'name' and 'price'.";
        }

        StringBuilder resultBuilder = new StringBuilder();
        int pricesSet = 0;

        for (JsonNode priceNode : params.get("prices")) {
            String itemName = priceNode.path("name").asText();
            double price = priceNode.path("price").asDouble(-1.0);

            try {
                ValidationUtils.validateItemName(itemName);
                ValidationUtils.validatePrice(price, config.getSimulation().getMinItemPrice(), config.getSimulation().getMaxItemPrice());
            } catch (IllegalArgumentException e) {
                resultBuilder.append("Failed to set price for ").append(itemName).append(": ").append(e.getMessage()).append("\n");
                continue;
            }

            Item vendingItem = state.getVendingMachine().getItem(itemName);
            Item storageItem = state.getStorage().getItem(itemName);

            if (vendingItem == null && storageItem == null) {
                resultBuilder.append("Failed to set price for ").append(itemName).append(": Item not found.\n");
            } else {
                if (vendingItem != null) {
                    vendingItem.setPrice(price);
                }
                if (storageItem != null) {
                    storageItem.setPrice(price);
                }
                pricesSet++;
                resultBuilder.append("Price for ").append(itemName).append(" set to $").append(String.format("%.2f", price)).append(".\n");
            }
        }

        if (pricesSet == 0) {
            return "Error: No valid items were provided to set prices for.";
        }
        return resultBuilder.toString().trim();
    }
}