package ai.timefold.solver.quarkus.config;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "timefold", phase = ConfigPhase.RUN_TIME)
public class TimefoldRuntimeConfig {
    /**
     * During run time, this is translated into Timefold's {@link SolverConfig}
     * runtime properties.
     */
    @ConfigItem
    public SolverRuntimeConfig solver;

    /**
     * Configuration properties that overwrite Timefold's {@link SolverManagerConfig}.
     */
    @ConfigItem
    public SolverManagerRuntimeConfig solverManager;
}
