package com.aiexpert.vendingbench.controller;

import com.aiexpert.vendingbench.controller.dto.HumanInputRequest;
import com.aiexpert.vendingbench.controller.dto.SimulationStartRequest;
import com.aiexpert.vendingbench.controller.dto.SimulationStateResponse;
import com.aiexpert.vendingbench.service.SimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationEngine simulationEngine;

    public SimulationController(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    @PostMapping("/start")
    public ResponseEntity<Void> startSimulation(@RequestBody SimulationStartRequest request) {
        simulationEngine.start(
            request.getProvider(),
            request.getApiKey(),
            request.getModelName(),
            request.getPersona(),
            request.getMaxTurns()
        );
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/resume")
    public ResponseEntity<Void> resumeWithHumanInput(@RequestBody HumanInputRequest request) {
        simulationEngine.resumeWithHumanInput(request.getPrompt());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/state")
    public ResponseEntity<SimulationStateResponse> getSimulationState() {
        SimulationStateResponse response = new SimulationStateResponse();
        response.setStatus(simulationEngine.getStatus());
        response.setMainAgentStatus(simulationEngine.getMainAgent().getStatus());
        response.setMainAgentThought(simulationEngine.getMainAgent().getLastThought());
        response.setSubAgentStatus(simulationEngine.getSubAgent().getStatus());
        response.setSimulationState(simulationEngine.getCurrentState());
        response.setNetWorth(simulationEngine.calculateNetWorth());
        response.setMainEventLog(simulationEngine.getMainEventLog());
        response.setVerboseLog(simulationEngine.getVerboseLog());
        if (simulationEngine.getCurrentState() != null) {
            response.setEmailInbox(simulationEngine.getCurrentState().getEmailInbox());
        }
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/sent-emails")
    public ResponseEntity<List<Map<String, String>>> getSentEmails() {
        if (simulationEngine.getCurrentState() != null) {
            return ResponseEntity.ok(simulationEngine.getCurrentState().getSentEmails());
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping("/pause")
    public ResponseEntity<Void> pauseSimulation() {
        simulationEngine.togglePause();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetSimulation() {
        simulationEngine.reset();
        return ResponseEntity.ok().build();
    }
}