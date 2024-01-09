package ai.timefold.solver.quarkus.config;

import java.util.Map;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.timefold")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TimefoldRuntimeConfig {

    String DEFAULT_SOLVER_NAME = "default";

    /**
     * During run time, this is translated into Timefold's {@link SolverConfig}
     * runtime properties.
     */
    @WithUnnamedKey(DEFAULT_SOLVER_NAME)
    @WithDefaults
    Map<String, SolverRuntimeConfig> solver();

    /**
     * Configuration properties that overwrite Timefold's {@link SolverManagerConfig}.
     */
    @WithDefaults
    SolverManagerRuntimeConfig solverManager();

    default Optional<SolverRuntimeConfig> getSolverRuntimeConfig(String solverName) {
        return Optional.ofNullable(solver().get(solverName));
    }
}
