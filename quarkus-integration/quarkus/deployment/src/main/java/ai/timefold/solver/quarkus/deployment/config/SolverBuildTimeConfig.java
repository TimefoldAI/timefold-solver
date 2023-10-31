package ai.timefold.solver.quarkus.deployment.config;

import java.util.Optional;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * During build time, this is translated into Timefold's {@link SolverConfig}
 * (except for termination properties which are translated at bootstrap time).
 *
 * See also {@link SolverRuntimeConfig}
 */
@ConfigGroup
public class SolverBuildTimeConfig {

    /**
     * Enable runtime assertions to detect common bugs in your implementation during development.
     * Defaults to {@link EnvironmentMode#REPRODUCIBLE}.
     */
    @ConfigItem
    public Optional<EnvironmentMode> environmentMode;

    /**
     * Enable daemon mode. In daemon mode, non-early termination pauses the solver instead of stopping it,
     * until the next problem fact change arrives. This is often useful for real-time planning.
     * Defaults to "false".
     */
    @ConfigItem
    public Optional<Boolean> daemon;

    /**
     * Determines how to access the fields and methods of domain classes.
     * Defaults to {@link DomainAccessType#GIZMO}.
     */
    @ConfigItem
    public Optional<DomainAccessType> domainAccessType;

    /**
     * What constraint stream implementation to use. Defaults to {@link ConstraintStreamImplType#BAVET}.
     *
     * @deprecated Not used anymore.
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "1.4.0")
    public Optional<ConstraintStreamImplType> constraintStreamImplType;

}
