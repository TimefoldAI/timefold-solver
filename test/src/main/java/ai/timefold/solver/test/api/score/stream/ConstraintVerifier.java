package ai.timefold.solver.test.api.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.test.impl.score.stream.DefaultConstraintVerifier;

import org.jspecify.annotations.NonNull;

/**
 * Implementations must be thread-safe, in order to enable parallel test execution.
 *
 * @param <ConstraintProvider_>
 * @param <Solution_>
 */
public interface ConstraintVerifier<ConstraintProvider_ extends ConstraintProvider, Solution_> {

    /**
     * Entry point to the API.
     *
     * @param constraintProvider {@link PlanningEntity} used by the {@link PlanningSolution}
     * @param planningSolutionClass {@link PlanningSolution}-annotated class associated with the constraints
     * @param entityClasses at least one, {@link PlanningEntity} types used by the {@link PlanningSolution}
     * @param <ConstraintProvider_> type of the {@link ConstraintProvider}
     * @param <Solution_> type of the {@link PlanningSolution}-annotated class
     */
    static <ConstraintProvider_ extends ConstraintProvider, Solution_>
            @NonNull ConstraintVerifier<ConstraintProvider_, Solution_> build(
                    @NonNull ConstraintProvider_ constraintProvider,
                    @NonNull Class<Solution_> planningSolutionClass, @NonNull Class<?> @NonNull... entityClasses) {
        requireNonNull(constraintProvider);
        var solutionDescriptor = SolutionDescriptor
                .buildSolutionDescriptor(requireNonNull(planningSolutionClass), entityClasses);
        return new DefaultConstraintVerifier<>(constraintProvider, solutionDescriptor);
    }

    /**
     * Uses a {@link SolverConfig} to build a {@link ConstraintVerifier}.
     * Alternative to {@link #build(ConstraintProvider, Class, Class[])}.
     *
     * @param solverConfig must have a {@link PlanningSolution} class, {@link PlanningEntity} classes
     *        and a {@link ConstraintProvider} configured.
     * @param <ConstraintProvider_> type of the {@link ConstraintProvider}
     * @param <Solution_> type of the {@link PlanningSolution}-annotated class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <ConstraintProvider_ extends ConstraintProvider, Solution_>
            @NonNull ConstraintVerifier<ConstraintProvider_, Solution_>
            create(@NonNull SolverConfig solverConfig) {
        var nonNullSolverConfig = requireNonNull(solverConfig);
        var entityClassList = Objects.requireNonNull(nonNullSolverConfig.getEntityClassList());
        var solutionDescriptor =
                SolutionDescriptor.buildSolutionDescriptor(requireNonNull(nonNullSolverConfig.getSolutionClass()),
                        entityClassList.toArray(new Class<?>[] {}));
        var scoreDirectorFactoryConfig = requireNonNull(nonNullSolverConfig.getScoreDirectorFactoryConfig());
        var constraintProviderClass = requireNonNull(scoreDirectorFactoryConfig.getConstraintProviderClass());
        var constraintProvider = ConfigUtils.newInstance(scoreDirectorFactoryConfig::toString, "constraintProviderClass",
                constraintProviderClass);
        ConfigUtils.applyCustomProperties(constraintProvider, "constraintProviderClass",
                scoreDirectorFactoryConfig.getConstraintProviderCustomProperties(), "constraintProviderCustomProperties");

        return new DefaultConstraintVerifier(constraintProvider, solutionDescriptor);
    }

    /**
     * All subsequent calls to {@link #verifyThat(BiFunction)} and {@link #verifyThat()}
     * use the given {@link ConstraintStreamImplType}.
     *
     * @return this
     * @deprecated There is only one implementation, so this method is deprecated.
     *             This method no longer has any effect.
     */
    @NonNull
    @Deprecated(forRemoval = true, since = "1.16.0")
    default ConstraintVerifier<ConstraintProvider_, Solution_> withConstraintStreamImplType(
            @NonNull ConstraintStreamImplType constraintStreamImplType) {
        return this;
    }

    /**
     * Creates a constraint verifier for a given {@link Constraint} of the {@link ConstraintProvider}.
     */
    @NonNull
    SingleConstraintVerification<Solution_> verifyThat(
            @NonNull BiFunction<ConstraintProvider_, ConstraintFactory, Constraint> constraintFunction);

    /**
     * Creates a constraint verifier for all constraints of the {@link ConstraintProvider}.
     */
    @NonNull
    MultiConstraintVerification<Solution_> verifyThat();

}
