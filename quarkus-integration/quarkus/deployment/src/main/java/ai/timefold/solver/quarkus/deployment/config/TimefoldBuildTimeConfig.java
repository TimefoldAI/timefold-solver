package ai.timefold.solver.quarkus.deployment.config;

import java.util.Map;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverConfig;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
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
     * Configuration properties that overwrite Timefold's {@link SolverConfig} per Solver.
     */
    @WithUnnamedKey(DEFAULT_SOLVER_NAME)
    @WithDefaults
    Map<String, SolverBuildTimeConfig> solver();

    default boolean hasOnlyDefaultSolverConfig() {
        return solver().size() == 1 && solver().containsKey(DEFAULT_SOLVER_NAME);
    }

    default Optional<SolverBuildTimeConfig> getDefaultSolverConfig() {
        return getSolverConfig(DEFAULT_SOLVER_NAME);
    }

    default Optional<SolverBuildTimeConfig> getSolverConfig(String solverName) {
        return Optional.ofNullable(solver().get(solverName));
    }

}
