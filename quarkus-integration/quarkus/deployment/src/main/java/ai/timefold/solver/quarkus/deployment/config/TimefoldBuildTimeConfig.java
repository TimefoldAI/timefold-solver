package ai.timefold.solver.quarkus.deployment.config;

import java.util.Map;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverConfig;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithUnnamedKey;

/**
 * During build time, this is translated into Timefold's Config classes.
 */
@ConfigMapping(prefix = "quarkus.timefold")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TimefoldBuildTimeConfig {

    String DEFAULT_SOLVER_CONFIG_URL = "solverConfig.xml";
    String DEFAULT_SOLVER_NAME = "default";

    /**
     * A classpath resource to read the solver configuration XML.
     * Defaults to {@value DEFAULT_SOLVER_CONFIG_URL}.
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    Optional<String> solverConfigXml();

    /**
     * Configuration properties that overwrite Timefold's {@link SolverConfig} per Solver. If a solver name is not
     * explicitly specified, the solver name will default to {@link #DEFAULT_SOLVER_NAME}.
     */
    @WithUnnamedKey(DEFAULT_SOLVER_NAME)
    Map<String, SolverBuildTimeConfig> solver();

    default boolean isDefaultSolverConfig(String solverName) {
        // 1 - No solver configuration, which means we will use a default empty SolverConfig and default Solver name
        // 2 - Only one solve config. It will be the default one.
        return solver().isEmpty() || solver().size() == 1 && getSolverConfig(solverName).isPresent();
    }

    default Optional<SolverBuildTimeConfig> getSolverConfig(String solverName) {
        return Optional.ofNullable(solver().get(solverName));
    }

}
