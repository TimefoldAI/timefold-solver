package ai.timefold.solver.core.preview.api.move;

import java.util.Collection;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A Move represents a change of 1 or more {@link PlanningVariable}s of 1 or more {@link PlanningEntity}s
 * in the working {@link PlanningSolution}.
 * <p>
 * Usually the move holds a direct reference to each {@link PlanningEntity} of the {@link PlanningSolution}
 * which it will change when {@link #execute(MutableSolutionView)} is called.
 * It is recommended for the moves to not touch shadow variables,
 * the solver will update shadow variables after move execution is complete.
 * If the move has to touch shadow variables, it is responsible for updating them
 * consistently across the entire dependency graph.
 * <p>
 * For tabu search, a Move should implement {@link Object#equals(Object)} and {@link Object#hashCode()},
 * {@link #getPlanningEntities()} and {@link #getPlanningValues()}.
 *
 * @param <Solution_>
 * @see MoveTester How to test {@link Move}s.
 */
@NullMarked
public interface Move<Solution_> {

    /**
     * Runs the move and optionally records the changes done,
     * so that they can be undone later.
     *
     * @param solutionView Exposes all possible mutative operations on the variables.
     *        Remembers those mutative operations and can replay them in reverse order
     *        when the solver needs to undo the move.
     *        Do not store this parameter in a field.
     */
    void execute(MutableSolutionView<Solution_> solutionView);

    /**
     * Rebases a move from an origin working solution
     * to another destination working solution which is usually on another {@link Thread}.
     * It is necessary for multithreaded solving to function.
     * <p>
     * The new move returned by this method translates the entities and problem facts
     * to the destination {@link PlanningSolution} of the destination.
     * That destination {@link PlanningSolution} is a deep planning clone (or an even deeper clone)
     * of the origin {@link PlanningSolution} that this move has been generated from.
     * <p>
     * That new move does the exact same change as this move,
     * resulting in the same {@link PlanningSolution} state,
     * presuming that destination {@link PlanningSolution} was in the same state
     * as the original {@link PlanningSolution} to begin with.
     * <p>
     * An implementation of this method typically iterates through every entity and fact instance in this move,
     * translates each one to the destination with {@link Lookup#lookUpWorkingObject(Object)}
     * and creates a new move instance of the same move type, using those translated instances.
     * If the working object isn't getting cloned, as many problem facts wouldn't be,
     * it doesn't need to be translated and can be reused in the new move instance as is.
     * <p>
     * The destination {@link PlanningSolution} can be in a different state than the original {@link PlanningSolution}.
     * So, rebasing can only depend on the identity of {@link PlanningEntity planning entities}
     * and {@link ProblemFactProperty problem facts},
     * which are usually declared by a {@link PlanningId} on those classes.
     * It must not depend on the state of the {@link PlanningVariable planning variables}.
     * One thread might rebase a move before, amid or after another thread does that same move instance.
     * <p>
     * The default implementation throws an {@link UnsupportedOperationException},
     * making multithreaded solving impossible unless the move class implements this method.
     *
     * @param lookup Do not store this parameter in a field
     * @return New move that does the same change as this move on another solution instance
     */
    default Move<Solution_> rebase(Lookup lookup) {
        throw new UnsupportedOperationException(
                "Move class (%s) doesn't implement the rebase() method, so multithreaded solving is impossible."
                        .formatted(getClass()));
    }

    /**
     * Returns all planning entities that this move is changing.
     * Required for entity tabu.
     * <p>
     * This method is only called after {@link #execute(MutableSolutionView)}, which might affect the return values.
     * Duplicate entries in the returned {@link Collection} are best avoided.
     * The default implementation throws an {@link UnsupportedOperationException},
     * making tabu search impossible unless the move class implements this method.
     *
     * @return Each entity only once.
     */
    default SequencedCollection<Object> getPlanningEntities() {
        throw new UnsupportedOperationException(
                "Move class (%s) doesn't implement the getPlanningEntities() method, so Entity Tabu Search is impossible."
                        .formatted(getClass()));
    }

    /**
     * Returns all planning values that this move is assigning to entity variables.
     * Required for value tabu.
     * <p>
     * This method is only called after {@link #execute(MutableSolutionView)}, which might affect the return values.
     * Duplicate entries in the returned {@link Collection} are best avoided.
     * The default implementation throws an {@link UnsupportedOperationException},
     * making tabu search impossible unless the move class implements this method.
     *
     * @return Each value only once. May contain null.
     */
    default SequencedCollection<@Nullable Object> getPlanningValues() {
        throw new UnsupportedOperationException(
                "Move class (%s) doesn't implement the getPlanningValues() method, so Value Tabu Search is impossible."
                        .formatted(getClass()));
    }

    /**
     * Describes the move type for statistical purposes.
     * For example, a move which changes a variable "computer" on a class "Process" could be described as
     * "ChangeMove(Process.computer)".
     * <p>
     * The format is not formalized, but it is recommended to stick to ASCII,
     * avoiding whitespace and special characters.
     * Never parse the {@link String} returned by this method,
     * it is only intended to be used by the solver.
     *
     * @return Non-empty {@link String} that describes the move type.
     *
     */
    default String describe() {
        return getClass().getSimpleName();
    }

}
