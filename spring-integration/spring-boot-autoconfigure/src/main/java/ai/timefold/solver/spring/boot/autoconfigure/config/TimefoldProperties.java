package ai.timefold.solver.spring.boot.autoconfigure.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(value = "timefold", ignoreUnknownFields = false)
public class TimefoldProperties {

    public static final String DEFAULT_SOLVER_CONFIG_URL = "solverConfig.xml";
    public static final String DEFAULT_SOLVER_BENCHMARK_CONFIG_URL = "solverBenchmarkConfig.xml";
    public static final String DEFAULT_SOLVER_NAME = "default";

    @NestedConfigurationProperty
    private SolverManagerProperties solverManager;

    /**
     * A classpath resource to read the solver configuration XML.
     * Defaults to solverConfig.xml.
     * If this property isn't specified, that file is optional.
     */
    private String solverConfigXml;

    @NestedConfigurationProperty
    private Map<String, SolverProperties> solver;

    @NestedConfigurationProperty
    private BenchmarkProperties benchmark;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public SolverManagerProperties getSolverManager() {
        return solverManager;
    }

    public void setSolverManager(SolverManagerProperties solverManager) {
        this.solverManager = solverManager;
    }

    public String getSolverConfigXml() {
        return solverConfigXml;
    }

    public void setSolverConfigXml(String solverConfigXml) {
        this.solverConfigXml = solverConfigXml;
    }

    public Map<String, SolverProperties> getSolver() {
        return solver;
    }

    public void setSolver(Map<String, Object> solver) {
        // Solver properties can be configured for a single solver or multiple solvers. The namespace timefold.solver.*
        // defines the default properties for a single solver and timefold.solver.solver-name.* allows configuring
        // multiple solvers
        this.solver = new HashMap<>();
        // Check if it is a single solver
        if (SolverProperty.getValidPropertyNames().containsAll(solver.keySet())) {
            SolverProperties solverProperties = new SolverProperties();
            solverProperties.loadProperties(solver);
            this.solver.put(DEFAULT_SOLVER_NAME, solverProperties);
        } else {
            // The values must be an instance of map
            var invalidKeySet = solver.entrySet().stream()
                    .filter(e -> e.getValue() != null && !(e.getValue() instanceof Map<?, ?>))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(TreeSet::new));
            if (!invalidKeySet.isEmpty()) {
                throw new IllegalStateException("""
                        Cannot use global solver properties with named solvers.
                        Expected all values to be maps, but values for key(s) %s are not map(s).
                        Maybe try changing the property name to kebab-case.
                        Here is the list of valid global solver properties: %s"""
                        .formatted(invalidKeySet, String.join(", ", SolverProperty.getValidPropertyNames())));
            }
            // Multiple solvers. We load the properties per key (or solver config)
            solver.forEach((key, value) -> {
                SolverProperties solverProperties = new SolverProperties();
                if (value != null) {
                    solverProperties.loadProperties((Map<String, Object>) value);
                }
                this.solver.put(key, solverProperties);
            });
        }
    }

    public BenchmarkProperties getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(BenchmarkProperties benchmark) {
        this.benchmark = benchmark;
    }

    public Optional<SolverProperties> getSolverConfig(String solverName) {
        return Optional.ofNullable(this.solver).map(s -> s.get(solverName));
    }
}
