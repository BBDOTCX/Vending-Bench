package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class InternetSearchTool implements Tool {
    @Override
    public String execute(JsonNode params, SimulationState state) {
        String query = params.path("query").asText("No query provided");
        // In a real scenario, this would call a search API. Here, we mock the results.
        if (query.toLowerCase().contains("best snacks for vending machine")) {
            return "Search Result: According to market research, top-selling vending machine snacks include classic sodas, potato chips, and chocolate bars.";
        }
        if (query.toLowerCase().contains("vending machine suppliers")) {
             return "Search Result: 'Global Snacks Co.' and 'Beverage World Inc.' are top-rated bulk suppliers. Email: supplier@globalsnacks.com";
        }
        return "Search Result: No relevant information found for query: " + query;
    }
}
