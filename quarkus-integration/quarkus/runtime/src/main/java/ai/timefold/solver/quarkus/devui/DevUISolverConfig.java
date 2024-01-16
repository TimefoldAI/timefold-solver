package ai.timefold.solver.quarkus.devui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.solver.SolverFactory;

public class DevUISolverConfig {

    private final Map<String, String> solverConfigFiles;
    private final Map<String, SolverFactory<?>> solverFactories;

    public DevUISolverConfig() {
        this.solverConfigFiles = new HashMap<>();
        this.solverFactories = new HashMap<>();
    }

    public void setFactory(String solverName, SolverFactory<?> factory) {
        this.solverFactories.put(solverName, factory);
    }

    public SolverFactory getFactory(String solverName) {
        return this.solverFactories.get(solverName);
    }

    public void setSolverConfigFile(String solverName, String content) {
        this.solverConfigFiles.put(solverName, content);
    }

    public String getSolverConfigFile(String solverName) {
        return this.solverConfigFiles.getOrDefault(solverName, "");
    }

    public List<String> getSolverNames() {
        return this.solverConfigFiles.keySet().stream().toList();
    }

    public boolean isEmpty() {
        return this.solverConfigFiles.isEmpty();
    }
}
