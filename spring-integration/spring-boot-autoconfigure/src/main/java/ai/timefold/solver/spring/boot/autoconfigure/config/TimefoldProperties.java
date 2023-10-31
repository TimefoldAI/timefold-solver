package ai.timefold.solver.spring.boot.autoconfigure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(value = "timefold", ignoreUnknownFields = false)
public class TimefoldProperties {

    public static final String DEFAULT_SOLVER_CONFIG_URL = "solverConfig.xml";
    public static final String DEFAULT_SOLVER_BENCHMARK_CONFIG_URL = "solverBenchmarkConfig.xml";

    @NestedConfigurationProperty
    private SolverManagerProperties solverManager;

    /**
     * A classpath resource to read the solver configuration XML.
     * Defaults to solverConfig.xml.
     * If this property isn't specified, that file is optional.
     */
    private String solverConfigXml;

    @NestedConfigurationProperty
    private SolverProperties solver;

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

    public SolverProperties getSolver() {
        return solver;
    }

    public void setSolver(SolverProperties solver) {
        this.solver = solver;
    }

    public BenchmarkProperties getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(BenchmarkProperties benchmark) {
        this.benchmark = benchmark;
    }
}
