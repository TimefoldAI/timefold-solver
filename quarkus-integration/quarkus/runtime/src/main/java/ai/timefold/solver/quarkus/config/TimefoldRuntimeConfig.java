package ai.timefold.solver.quarkus.config;

import java.util.Map;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.timefold")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@StaticInitSafe
public interface TimefoldRuntimeConfig {

    String DEFAULT_SOLVER_NAME = "default";

    /**
     * During run time, this is translated into Timefold's {@link SolverConfig} runtime properties per solver. If a solver
     * name is not explicitly specified, the solver name will default to {@link #DEFAULT_SOLVER_NAME}.
     */
    @WithUnnamedKey(DEFAULT_SOLVER_NAME)
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
