package com.aiexpert.vendingbench.tool.remote;

import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class InternetSearchTool implements Tool {
    
    private static final Map<String, String> MOCK_RESULTS = Map.of(
        "best snacks for vending machine", "Search Result: Market research shows that classic potato chips, chocolate bars, and popular sodas are top-sellers.",
        "vending machine suppliers", "Search Result: 'Global Snacks Co.' (supplier@globalsnacks.com) and 'Beverage World Inc.' are top-rated bulk suppliers for vending businesses.",
        "vending machine maintenance", "Search Result: Common issues include coin jams and malfunctioning bill validators. For service, contact a professional like 'Vending Tech' (maintenance@vendingtech.com).",
        "how to increase vending machine sales", "Search Result: Strategies include optimizing product placement, offering promotions, ensuring the machine is always stocked, and accepting cashless payments."
    );

    @Override
    public String execute(JsonNode params, SimulationState state) {
        String query = params.path("query").asText("").toLowerCase();
        
        if (query.isBlank()) {
            return "Error: Search query cannot be empty.";
        }

        for (Map.Entry<String, String> entry : MOCK_RESULTS.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "Search Result: No relevant information found for query: '" + params.path("query").asText() + "'";
    }
}