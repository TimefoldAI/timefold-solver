package ai.timefold.solver.benchmark.quarkus.config;

import ai.timefold.solver.quarkus.config.TerminationRuntimeConfig;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "timefold.benchmark", phase = ConfigPhase.RUN_TIME)
public class TimefoldBenchmarkRuntimeConfig {
    public static final String DEFAULT_BENCHMARK_RESULT_DIRECTORY = "target/benchmarks";

    /**
     * Where the benchmark results are written to. Defaults to
     * {@link DEFAULT_BENCHMARK_RESULT_DIRECTORY}.
     */
    @ConfigItem(defaultValue = DEFAULT_BENCHMARK_RESULT_DIRECTORY)
    public String resultDirectory;

    /**
     * Termination configuration for the solvers run in the benchmark.
     */
    @ConfigItem(name = "solver.termination")
    public TerminationRuntimeConfig termination;
}
