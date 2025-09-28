package com.aiexpert.vendingbench.tool.vending;

import com.aiexpert.vendingbench.model.SimulationState;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectCashToolTest {

    private CollectCashTool collectCashTool;
    private SimulationState state;

    @BeforeEach
    void setUp() {
        collectCashTool = new CollectCashTool();
        // Assuming SimulationState constructor sets initial values, which it does.
        state = new SimulationState(500.0, 2.0);
    }

    @Test
    void testExecute_WithCashInMachine_ShouldTransferToBalance() {
        // Arrange
        state.getVendingMachine().setCashHeld(150.75);
        state.setCashBalance(500.0);

        // Act
        String result = collectCashTool.execute(JsonNodeFactory.instance.objectNode(), state);

        // Assert
        assertEquals(0.0, state.getVendingMachine().getCashHeld());
        assertEquals(650.75, state.getCashBalance());
        assertTrue(result.contains("Collected $150.75"));
        assertTrue(result.contains("new main balance is $650.75"));
    }

    @Test
    void testExecute_WithZeroCashInMachine_ShouldDoNothing() {
        // Arrange
        state.getVendingMachine().setCashHeld(0.0);
        state.setCashBalance(500.0);

        // Act
        String result = collectCashTool.execute(JsonNodeFactory.instance.objectNode(), state);

        // Assert
        assertEquals(0.0, state.getVendingMachine().getCashHeld());
        assertEquals(500.0, state.getCashBalance());
        assertEquals("No cash to collect from the vending machine.", result);
    }

    @Test
    void testExecute_WithNegativeCashInMachine_ShouldHandleGracefully() {
        // Arrange (edge case)
        state.getVendingMachine().setCashHeld(-50.0);
        state.setCashBalance(500.0);

        // Act
        String result = collectCashTool.execute(JsonNodeFactory.instance.objectNode(), state);

        // Assert
        assertEquals(-50.0, state.getVendingMachine().getCashHeld());
        assertEquals(500.0, state.getCashBalance());
        assertEquals("No cash to collect from the vending machine.", result);
    }
}