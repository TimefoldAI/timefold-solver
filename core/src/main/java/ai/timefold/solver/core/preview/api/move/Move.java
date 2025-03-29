package ai.timefold.solver.core.preview.api.move;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NullMarked;

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
 * {@link #extractPlanningEntities()} and {@link #extractPlanningValues()}.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 * 
 * @param <Solution_>
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
     * Rebases a move from an origin {@link ScoreDirector} to another destination {@link ScoreDirector}
     * which is usually on another {@link Thread}.
     * It is necessary for multithreaded solving to function.
     * <p>
     * The new move returned by this method translates the entities and problem facts
     * to the destination {@link PlanningSolution} of the destination {@link ScoreDirector},
     * That destination {@link PlanningSolution} is a deep planning clone (or an even deeper clone)
     * of the origin {@link PlanningSolution} that this move has been generated from.
     * <p>
     * That new move does the exact same change as this move,
     * resulting in the same {@link PlanningSolution} state,
     * presuming that destination {@link PlanningSolution} was in the same state
     * as the original {@link PlanningSolution} to begin with.
     * <p>
     * An implementation of this method typically iterates through every entity and fact instance in this move,
     * translates each one to the destination {@link ScoreDirector} with {@link Rebaser#rebase(Object)}
     * and creates a new move instance of the same move type, using those translated instances.
     * <p>
     * The destination {@link PlanningSolution} can be in a different state than the original {@link PlanningSolution}.
     * So, rebasing can only depend on the identity of {@link PlanningEntity planning entities}
     * and {@link ProblemFactProperty problem facts},
     * which are usually declared by a {@link PlanningId} on those classes.
     * It must not depend on the state of the {@link PlanningVariable planning variables}.
     * One thread might rebase a move before, amid or after another thread does that same move instance.
     * <p>
     * This method is thread-safe.
     *
     * @param rebaser Do not store this parameter in a field
     * @return New move that does the same change as this move on another solution instance
     */
    Move<Solution_> rebase(Rebaser rebaser);

    /**
     * Returns all planning entities that this move is changing.
     * Required for entity tabu.
     * <p>
     * This method is only called after {@link #execute(MutableSolutionView)}, which might affect the return values.
     * <p>
     * Duplicate entries in the returned {@link Collection} are best avoided.
     * The returned {@link Collection} is recommended to be in a stable order.
     * For example, use {@link List} or {@link LinkedHashSet}, but not {@link HashSet}.
     *
     * @return Each entity only once.
     */
    default Collection<?> extractPlanningEntities() {
        throw new UnsupportedOperationException("The move (" + this + ") does not support tabu search.");
    }

    /**
     * Returns all planning values that this move is assigning to entity variables.
     * Required for value tabu.
     * <p>
     * This method is only called after {@link #execute(MutableSolutionView)}, which might affect the return values.
     * <p>
     * Duplicate entries in the returned {@link Collection} are best avoided.
     * The returned {@link Collection} is recommended to be in a stable order.
     * For example, use {@link List} or {@link LinkedHashSet}, but not {@link HashSet}.
     *
     * @return Each value only once.
     */
    default Collection<?> extractPlanningValues() {
        throw new UnsupportedOperationException("The move (" + this + ") does not support tabu search.");
    }

    /**
     * Describes the move type for statistical purposes.
     * For example, a move which changes a variable "computer" on a class "Process" could be described as
     * "ChangeMove(Process.computer)".
     * <p>
     * The format is not formalized.
     * Never parse the {@link String} returned by this method.
     *
     * @return Non-empty {@link String} that describes the move type.
     */
    default String describe() {
        return getClass().getSimpleName();
    }

    /**
     * The solver will make sure to only call this when the move is actually printed out during debug logging.
     *
     * @return A description of the move, ideally including the state of the planning entities being changed.
     */
    @Override
    String toString();

}
