package com.aiexpert.vendingbench.util;

import com.aiexpert.vendingbench.model.Inventory;
import com.aiexpert.vendingbench.model.SimulationState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.stream.Collectors;

@Component
public class StateCloner {

    public SimulationState clone(SimulationState original) {
        if (original == null) {
            return null;
        }

        // Create a new state with the same initial parameters (they don't change)
        SimulationState clone = new SimulationState(0, original.getDailyFee());

        // Copy primitive and immutable fields
        clone.setTurn(original.getTurn());
        clone.setDay(original.getDay());
        clone.setCashBalance(original.getCashBalance());
        clone.setTotalUnitsSold(original.getTotalUnitsSold());

        // Deep copy mutable objects
        clone.setStorage(cloneInventory(original.getStorage()));
        clone.setVendingMachine(cloneInventory(original.getVendingMachine()));

        // Deep copy lists of maps
        clone.setEmailInbox(original.getEmailInbox().stream().map(HashMap::new).collect(Collectors.toList()));
        clone.setSentEmails(original.getSentEmails().stream().map(HashMap::new).collect(Collectors.toList()));

        // Copy pending deliveries
        original.getPendingDeliveries().forEach((day, items) -> 
            clone.getPendingDeliveries().put(day, new HashMap<>(items))
        );

        return clone;
    }

    private Inventory cloneInventory(Inventory original) {
        Inventory clone = new Inventory();
        clone.setCashHeld(original.getCashHeld());
        // getClonedItems() already performs a deep copy of the items map
        clone.setItems(original.getClonedItems());
        return clone;
    }
}