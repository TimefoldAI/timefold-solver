package ai.timefold.solver.quarkus.deployment.config;

import java.util.Optional;
import java.util.Set;

import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * During build time, this is translated into Timefold's {@link SolverConfig}
 * (except for termination properties which are translated at bootstrap time).
 *
 * @see SolverRuntimeConfig
 */
@ConfigGroup
public interface SolverBuildTimeConfig {

    /**
     * A classpath resource to read the specific solver configuration XML.
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    // Build time - classes in the SolverConfig are visited by SolverConfig.visitReferencedClasses
    // which generates the constructor of classes used by Quarkus
    Optional<String> solverConfigXml();

    /**
     * Enable the Nearby Selection quick configuration.
     */
    // Build time - visited by SolverConfig.visitReferencedClasses
    // which generates the constructor used by Quarkus
    Optional<Class<?>> nearbyDistanceMeterClass();

    /**
     * What preview features to enable.
     * The list of available preview features should not
     * be considered stable and may change between releases.
     */
    Optional<Set<PreviewFeature>> enabledPreviewFeatures();

    /**
     * If constraint profiling is enabled. Defaults to false.
     */
    Optional<Boolean> constraintStreamProfilingEnabled();

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
    // Build time - modifies the ConstraintProvider class if set
    Optional<Boolean> constraintStreamAutomaticNodeSharing();
}
