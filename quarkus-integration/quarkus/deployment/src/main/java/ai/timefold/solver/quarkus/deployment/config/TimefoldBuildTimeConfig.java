package ai.timefold.solver.quarkus.deployment.config;

import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverConfig;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * During build time, this is translated into Timefold's Config classes.
 */
@ConfigRoot(name = "timefold")
public class TimefoldBuildTimeConfig {

    public static final String DEFAULT_SOLVER_CONFIG_URL = "solverConfig.xml";

    /**
     * A classpath resource to read the solver configuration XML.
     * Defaults to {@value DEFAULT_SOLVER_CONFIG_URL}.
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    @ConfigItem
    public Optional<String> solverConfigXml;

    /**
     * Configuration properties that overwrite Timefold's {@link SolverConfig}.
     */
    @ConfigItem
    public SolverBuildTimeConfig solver;

}
