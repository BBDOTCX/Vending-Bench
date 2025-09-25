package com.aiexpert.vendingbench.tool.vending;

import com.aiexpert.vendingbench.model.Inventory;
import com.aiexpert.vendingbench.model.SimulationState;
import com.aiexpert.vendingbench.tool.Tool;
import com.fasterxml.jackson.databind.JsonNode;

public class CollectCashTool implements Tool {
    @Override
    public String execute(JsonNode params, SimulationState state) {
        Inventory machine = state.getVendingMachine();
        double cashCollected = machine.getCashHeld();
        if (cashCollected <= 0) {
            return "No cash to collect from the vending machine.";
        }
        machine.setCashHeld(0);
        state.setCashBalance(state.getCashBalance() + cashCollected);
        return String.format("Collected $%.2f from the vending machine. New main balance is $%.2f.", cashCollected, state.getCashBalance());
    }
}
