package ai.timefold.solver.core.api.domain.solution;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;

/**
 * Specifies that a property (or a field) on a {@link PlanningSolution} class holds the {@link Score} of that solution.
 * <p>
 * This property can be null if the {@link PlanningSolution} is uninitialized.
 * <p>
 * This property is modified by the {@link Solver},
 * every time when the {@link Score} of this {@link PlanningSolution} has been calculated.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface PlanningScore {

    /**
     * Required for bendable scores.
     * <p>
     * For example with 3 hard levels, hard level 0 always outweighs hard level 1 which always outweighs hard level 2,
     * which outweighs all the soft levels.
     *
     * @return 0 or higher if the {@link Score} is a {@link IBendableScore}, not used otherwise
     */
    int bendableHardLevelsSize() default -1;

    /**
     * Required for bendable scores.
     * <p>
     * For example with 3 soft levels, soft level 0 always outweighs soft level 1 which always outweighs soft level 2.
     *
     * @return 0 or higher if the {@link Score} is a {@link IBendableScore}, not used otherwise
     */
    int bendableSoftLevelsSize() default -1;

}
