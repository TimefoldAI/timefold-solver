package ai.timefold.solver.benchmark.quarkus.config;

import ai.timefold.solver.quarkus.config.TerminationRuntimeConfig;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "timefold.benchmark")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TimefoldBenchmarkRuntimeConfig {
    String DEFAULT_BENCHMARK_RESULT_DIRECTORY = "target/benchmarks";

    /**
     * Where the benchmark results are written to. Defaults to
     * {@link #DEFAULT_BENCHMARK_RESULT_DIRECTORY}.
     */
    @WithDefault(DEFAULT_BENCHMARK_RESULT_DIRECTORY)
    String resultDirectory();

    /**
     * Termination configuration for the solvers run in the benchmark.
     */
    @WithName("solver.termination")
    TerminationRuntimeConfig termination();
}
