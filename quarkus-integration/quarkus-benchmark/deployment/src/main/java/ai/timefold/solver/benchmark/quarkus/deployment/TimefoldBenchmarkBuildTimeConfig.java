package ai.timefold.solver.benchmark.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * During build time, this is translated into Timefold's Config classes.
 */
@ConfigRoot(name = "timefold.benchmark")
public class TimefoldBenchmarkBuildTimeConfig {

    public static final String DEFAULT_SOLVER_BENCHMARK_CONFIG_URL = "solverBenchmarkConfig.xml";
    /**
     * A classpath resource to read the benchmark configuration XML.
     * Defaults to {@value DEFAULT_SOLVER_BENCHMARK_CONFIG_URL}.
     * If this property isn't specified, that solverBenchmarkConfig.xml is optional.
     */
    @ConfigItem
    Optional<String> solverBenchmarkConfigXml;
}
