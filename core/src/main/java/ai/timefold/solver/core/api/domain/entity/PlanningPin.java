package ai.timefold.solver.core.api.domain.entity;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

/**
 * Specifies that a boolean property (or field) of a {@link PlanningEntity} determines if the planning entity is pinned.
 * A pinned planning entity is never changed during planning;
 * to change a pinned planning entity, even to make it not pinned anymore, trigger a {@link ProblemChange}.
 * For example, it allows the user to pin a shift to a specific employee before solving
 * and the solver will not undo that, regardless of the constraints.
 * <p>
 * The boolean is false if the planning entity is movable and true if the planning entity is pinned.
 * <p>
 * The boolean property may also declare a setter that takes the planning solution,
 * for example {@code void setPinned(MySolution solution)}.
 * Timefold Solver calls that setter once when the working solution is set, passing the solution,
 * and the setter writes the boolean.
 * Later pin checks read the boolean, so the decision is cached.
 * A {@link ProblemChange} that touches the entity, or that changes other problem data,
 * calls the setter again and replaces the cached boolean.
 * A setter that only accepts the boolean itself is not called by the solver.
 * <p>
 * It applies to all the planning variables of that planning entity.
 * If set on an entity with {@link PlanningListVariable},
 * this will pin the entire list of planning values as well.
 * 
 * @see PlanningPinToIndex Read more about how to only pin part of the planning list variable.
 * @see ProblemChange Use ProblemChange to trigger pinning changes.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface PlanningPin {

}
