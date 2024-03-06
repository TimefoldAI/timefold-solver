package ai.timefold.solver.quarkus.deployment.config;

import java.util.Optional;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;
import ai.timefold.solver.quarkus.config.TerminationRuntimeConfig;

import io.quarkus.runtime.annotations.ConfigGroup;

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
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    Optional<String> solverConfigXml();

    /**
     * Enable runtime assertions to detect common bugs in your implementation during development.
     * Defaults to {@link EnvironmentMode#REPRODUCIBLE}.
     */
    Optional<EnvironmentMode> environmentMode();

    /**
     * Enable daemon mode. In daemon mode, non-early termination pauses the solver instead of stopping it,
     * until the next problem fact change arrives.
     * This is often useful for real-time planning.
     * Defaults to "false".
     */
    Optional<Boolean> daemon();

    /**
     * Determines how to access the fields and methods of domain classes.
     * Defaults to {@link DomainAccessType#GIZMO}.
     */
    Optional<DomainAccessType> domainAccessType();

    /**
     * Note: this setting is only available
     * for <a href="https://timefold.ai/docs/timefold-solver/latest/enterprise-edition/enterprise-edition">Timefold Solver
     * Enterprise Edition</a>.
     * Enable multithreaded solving for a single problem, which increases CPU consumption.
     * Defaults to {@value SolverConfig#MOVE_THREAD_COUNT_NONE}.
     * Other options include {@value SolverConfig#MOVE_THREAD_COUNT_AUTO}, a number
     * or formula based on the available processor count.
     */
    Optional<String> moveThreadCount();

    /**
     * Configuration properties regarding {@link TerminationConfig}.
     */
    TerminationRuntimeConfig termination();

    /**
     * Enable the Nearby Selection quick configuration.
     */
    Optional<Class<?>> nearbyDistanceMeterClass();

    /**
     * What constraint stream implementation to use. Defaults to {@link ConstraintStreamImplType#BAVET}.
     *
     * @deprecated Not used anymore.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    Optional<ConstraintStreamImplType> constraintStreamImplType();

    /**
     * Note: this setting is only available
     * for <a href="https://timefold.ai/docs/timefold-solver/latest/enterprise-edition/enterprise-edition">Timefold Solver
     * Enterprise Edition</a>.
     * Enable rewriting the {@link ai.timefold.solver.core.api.score.stream.ConstraintProvider} class
     * so nodes share lambdas when possible, improving performance.
     * When enabled, breakpoints placed in the {@link ai.timefold.solver.core.api.score.stream.ConstraintProvider}
     * will no longer be triggered.
     * Defaults to "false".
     */
    Optional<Boolean> constraintStreamAutomaticNodeSharing();
}
