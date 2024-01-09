package ai.timefold.solver.quarkus.deployment.config;

import java.util.Optional;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * During build time, this is translated into Timefold's {@link SolverConfig}
 * (except for termination properties which are translated at bootstrap time).
 *
 * See also {@link SolverRuntimeConfig}
 */
@ConfigGroup
public interface SolverBuildTimeConfig {

    /**
     * A classpath resource to read the specific solver configuration XML.
     *
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    Optional<String> solverConfigXml();

    /**
     * Enable runtime assertions to detect common bugs in your implementation during development.
     * Defaults to {@link EnvironmentMode#REPRODUCIBLE}.
     */
    @WithDefault("REPRODUCIBLE")
    Optional<EnvironmentMode> environmentMode();

    /**
     * Enable daemon mode. In daemon mode, non-early termination pauses the solver instead of stopping it,
     * until the next problem fact change arrives. This is often useful for real-time planning.
     * Defaults to "false".
     */
    @WithDefault("false")
    Optional<Boolean> daemon();

    /**
     * Determines how to access the fields and methods of domain classes.
     * Defaults to {@link DomainAccessType#GIZMO}.
     */
    @WithDefault("GIZMO")
    Optional<DomainAccessType> domainAccessType();

    /**
     * What constraint stream implementation to use. Defaults to {@link ConstraintStreamImplType#BAVET}.
     *
     * @deprecated Not used anymore.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    Optional<ConstraintStreamImplType> constraintStreamImplType();

}
