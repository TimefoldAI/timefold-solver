package ai.timefold.solver.benchmark.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * During build time, this is translated into Timefold's Config classes.
 */
@ConfigMapping(prefix = "quarkus.timefold.benchmark")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TimefoldBenchmarkBuildTimeConfig {

    String DEFAULT_SOLVER_BENCHMARK_CONFIG_URL = "solverBenchmarkConfig.xml";

    /**
     * A classpath resource to read the benchmark configuration XML.
     * Defaults to {@value DEFAULT_SOLVER_BENCHMARK_CONFIG_URL}.
     * If this property isn't specified, that solverBenchmarkConfig.xml is optional.
     */
    Optional<String> solverBenchmarkConfigXml();
}
