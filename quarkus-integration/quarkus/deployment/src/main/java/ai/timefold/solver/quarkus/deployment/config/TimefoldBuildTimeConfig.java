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
        // 3 - There is a Solver name set to default
        // 4 - If all the previous conditions do not apply, we will select the first key in ascending order. This ensures
        //     that the default bean is always set and does not break the existing beans that expect a default bean.
        return solver().isEmpty() || solver().size() == 1 && getSolverConfig(solverName).isPresent()
                || solver().containsKey(DEFAULT_SOLVER_NAME) && solverName.equals(DEFAULT_SOLVER_NAME)
                || solver().keySet().stream().sorted().findFirst().get().equals(solverName);
    }

    default Optional<SolverBuildTimeConfig> getSolverConfig(String solverName) {
        return Optional.ofNullable(solver().get(solverName));
    }

}
