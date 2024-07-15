package ai.timefold.solver.core.api.domain.solution;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

/**
 * Specifies that the class is a planning solution.
 * A solution represents a problem and a possible solution of that problem.
 * A possible solution does not need to be optimal or even feasible.
 * A solution's planning variables might not be initialized (especially when delivered as a problem).
 * <p>
 * A solution is mutable.
 * For scalability reasons (to facilitate incremental score calculation),
 * the same solution instance (called the working solution per move thread) is continuously modified.
 * It's cloned to recall the best solution.
 * <p>
 * Each planning solution must have exactly 1 {@link PlanningScore} property.
 * <p>
 * Each planning solution must have at least 1 {@link PlanningEntityCollectionProperty}
 * or {@link PlanningEntityProperty} property.
 * <p>
 * Each planning solution is recommended to have 1 {@link ConstraintWeightOverrides} property too.
 * This will make it easy for a solution to override constraint weights provided in {@link ConstraintProvider},
 * in turn making it possible to run different solutions with a different balance of constraint weights.
 * <p>
 * Each planning solution used with ConstraintStream score calculation must have at least 1
 * {@link ProblemFactCollectionProperty}
 * or {@link ProblemFactProperty} property.
 * <p>
 * The class should have a public no-arg constructor, so it can be cloned
 * (unless the {@link #solutionCloner()} is specified).
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface PlanningSolution {

    /**
     * Enable reflection through the members of the class
     * to automatically assume {@link PlanningScore}, {@link PlanningEntityCollectionProperty},
     * {@link PlanningEntityProperty}, {@link ProblemFactCollectionProperty}, {@link ProblemFactProperty}
     * and {@link ConstraintWeightOverrides} annotations based on the member type.
     *
     * <p>
     * This feature is not supported under Quarkus.
     * When using Quarkus,
     * setting this to anything other than {@link AutoDiscoverMemberType#NONE} will result in a build-time exception.
     *
     * @return never null
     */
    AutoDiscoverMemberType autoDiscoverMemberType() default AutoDiscoverMemberType.NONE;

    /**
     * Overrides the default {@link SolutionCloner} to implement a custom {@link PlanningSolution} cloning implementation.
     * <p>
     * If this is not specified, then the default reflection-based {@link SolutionCloner} is used,
     * so you don't have to worry about it.
     *
     * @return {@link NullSolutionCloner} when it is null (workaround for annotation limitation)
     */
    Class<? extends SolutionCloner> solutionCloner() default NullSolutionCloner.class;

    /** Workaround for annotation limitation in {@link #solutionCloner()}. */
    interface NullSolutionCloner extends SolutionCloner {
    }

    /**
     * @deprecated When multi-threaded solving, ensure your domain classes use @{@link PlanningId} instead.
     * @return never null
     */
    @Deprecated(forRemoval = true, since = "1.10.0")
    LookUpStrategyType lookUpStrategyType() default LookUpStrategyType.PLANNING_ID_OR_NONE;

}
