package ai.timefold.solver.spring.boot.autoconfigure.config;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class BenchmarkProperties {

    public static final String DEFAULT_SOLVER_BENCHMARK_CONFIG_URL = TimefoldProperties.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL;
    public static final String DEFAULT_BENCHMARK_RESULT_DIRECTORY = "target/benchmarks";

    /**
     * A classpath resource to read the benchmark configuration XML.
     * Defaults to solverBenchmarkConfig.xml.
     * If this property isn't specified, the file is optional.
     */
    private String solverBenchmarkConfigXml;

    /**
     * The directory to which to write the benchmark HTML report and graphs,
     * relative from the working directory.
     */
    private String resultDirectory;

    @NestedConfigurationProperty
    private BenchmarkSolverProperties solver;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public String getSolverBenchmarkConfigXml() {
        return solverBenchmarkConfigXml;
    }

    public void setSolverBenchmarkConfigXml(String solverBenchmarkConfigXml) {
        this.solverBenchmarkConfigXml = solverBenchmarkConfigXml;
    }

    public String getResultDirectory() {
        return resultDirectory;
    }

    public void setResultDirectory(String resultDirectory) {
        this.resultDirectory = resultDirectory;
    }

    public BenchmarkSolverProperties getSolver() {
        return solver;
    }

    public void setSolver(BenchmarkSolverProperties solver) {
        this.solver = solver;
    }

}
