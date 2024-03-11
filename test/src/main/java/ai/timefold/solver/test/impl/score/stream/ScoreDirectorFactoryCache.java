package ai.timefold.solver.test.impl.score.stream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.InnerConstraintFactory;

/**
 * Designed for access from a single thread.
 * Callers are responsible for ensuring that instances are never run from a thread other than that which created them.
 *
 * @param <ConstraintProvider_>
 * @param <Solution_>
 * @param <Score_>
 */
final class ScoreDirectorFactoryCache<ConstraintProvider_ extends ConstraintProvider, Solution_, Score_ extends Score<Score_>> {

    /**
     * Score director factory creation is expensive; we cache it.
     * The cache needs to be recomputed every time that the parent's configuration changes.
     */
    private final Map<ConstraintRef, AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_>> scoreDirectorFactoryMap =
            new HashMap<>();

    private final SolutionDescriptor<Solution_> solutionDescriptor;

    public ScoreDirectorFactoryCache(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
    }

    /**
     * Retrieve {@link AbstractConstraintStreamScoreDirectorFactory} from the cache,
     * or create and cache a new instance.
     * Cache key is the ID of the single constraint returned by calling the constraintFunction.
     *
     * @param constraintFunction never null, determines the single constraint to be used from the constraint provider
     * @param constraintProvider never null, determines the constraint provider to be used
     * @return never null
     */
    public AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory(
            BiFunction<ConstraintProvider_, ConstraintFactory, Constraint> constraintFunction,
            ConstraintProvider_ constraintProvider, EnvironmentMode environmentMode) {
        /*
         * Apply all validations on the constraint factory before extracting the one constraint.
         * This step is only necessary to perform validation of the constraint provider;
         * if we only wanted the one constraint, we could just call constraintFunction directly.
         */
        InnerConstraintFactory<Solution_, ?> fullConstraintFactory =
                new BavetConstraintFactory<>(solutionDescriptor, environmentMode);
        List<Constraint> constraints = (List<Constraint>) fullConstraintFactory.buildConstraints(constraintProvider);
        Constraint expectedConstraint = constraintFunction.apply(constraintProvider, fullConstraintFactory);
        Constraint result = constraints.stream()
                .filter(c -> Objects.equals(c.getConstraintRef(), expectedConstraint.getConstraintRef()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Impossible state: Constraint provider (" + constraintProvider
                        + ") has no constraint (" + expectedConstraint + ")."));
        return getScoreDirectorFactory(result.getConstraintRef(),
                constraintFactory -> new Constraint[] {
                        result
                }, environmentMode);
    }

    /**
     * Retrieve {@link AbstractConstraintStreamScoreDirectorFactory} from the cache,
     * or create and cache a new instance.
     *
     * @param constraintRef never null, unique identifier of the factory in the cache
     * @param constraintProvider never null, constraint provider to create the factory from; ignored on cache hit
     * @return never null
     */
    public AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory(ConstraintRef constraintRef,
            ConstraintProvider constraintProvider, EnvironmentMode environmentMode) {
        return scoreDirectorFactoryMap.computeIfAbsent(constraintRef,
                k -> new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor, constraintProvider, environmentMode));
    }

}
