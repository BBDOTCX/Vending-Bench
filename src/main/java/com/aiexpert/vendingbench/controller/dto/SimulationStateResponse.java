package com.aiexpert.vendingbench.controller.dto;

import com.aiexpert.vendingbench.model.SimulationState;
import java.util.List;
import java.util.Map;

public class SimulationStateResponse {
    private String status;
    private String mainAgentStatus;
    private String mainAgentThought;
    private String subAgentStatus;
    private SimulationState simulationState;
    private double netWorth;
    private List<String> mainEventLog;
    // private List<LogEntry> verboseLog; // REMOVED
    private List<Map<String, String>> emailInbox;

    // Getters and Setters (verboseLog getter/setter removed)
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMainAgentStatus() { return mainAgentStatus; }
    public void setMainAgentStatus(String mainAgentStatus) { this.mainAgentStatus = mainAgentStatus; }
    public String getMainAgentThought() { return mainAgentThought; }
    public void setMainAgentThought(String mainAgentThought) { this.mainAgentThought = mainAgentThought; }
    public String getSubAgentStatus() { return subAgentStatus; }
    public void setSubAgentStatus(String subAgentStatus) { this.subAgentStatus = subAgentStatus; }
    public SimulationState getSimulationState() { return simulationState; }
    public void setSimulationState(SimulationState simulationState) { this.simulationState = simulationState; }
    public double getNetWorth() { return netWorth; }
    public void setNetWorth(double netWorth) { this.netWorth = netWorth; }
    public List<String> getMainEventLog() { return mainEventLog; }
    public void setMainEventLog(List<String> mainEventLog) { this.mainEventLog = mainEventLog; }
    public List<Map<String, String>> getEmailInbox() { return emailInbox; }
    public void setEmailInbox(List<Map<String, String>> emailInbox) { this.emailInbox = emailInbox; }
}