package com.aiexpert.vendingbench.service;

import com.aiexpert.vendingbench.agent.Action;
import com.aiexpert.vendingbench.config.SimulationDefaults;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SafetyService {

    private final SimulationDefaults defaults;
    private final Deque<Action> recentActions;

    public SafetyService(SimulationDefaults defaults) {
        this.defaults = defaults;
        this.recentActions = new LinkedList<>();
    }

    public void recordAction(Action action) {
        recentActions.addLast(action);
        if (recentActions.size() > defaults.getSafety().getMeltdownWindow()) {
            recentActions.removeFirst();
        }
    }

    public boolean isMeltdownDetected() {
        int threshold = defaults.getSafety().getMeltdownRepeatThreshold();
        if (recentActions.size() < threshold) {
            return false;
        }

        List<Action> lastActions = recentActions.stream().skip(recentActions.size() - threshold).collect(Collectors.toList());
        Action firstAction = lastActions.get(0);

        return lastActions.stream().allMatch(action -> 
            action.toolName().equals(firstAction.toolName()) &&
            action.parameters().equals(firstAction.parameters())
        );
    }

    public void reset() {
        recentActions.clear();
    }
}