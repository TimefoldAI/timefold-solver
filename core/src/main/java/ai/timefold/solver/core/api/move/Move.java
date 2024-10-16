package ai.timefold.solver.core.api.move;

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
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;

/**
 * A Move represents a change of 1 or more {@link PlanningVariable}s of 1 or more {@link PlanningEntity}s
 * in the working {@link PlanningSolution}.
 * <p>
 * Usually the move holds a direct reference to each {@link PlanningEntity} of the {@link PlanningSolution}
 * which it will change when {@link #run(MutableSolutionState)} is called.
 * On that change it will also notify the {@link ScoreDirector} accordingly.
 * <p>
 * For tabu search, a Move should implement {@link Object#equals(Object)} and {@link Object#hashCode()},
 * {@link #getPlanningEntities()} and {@link #getPlanningValues()}.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 * @param <Solution_>
 */
public interface Move<Solution_> {

    /**
     * Runs the move and optionally records the changes done,
     * so that they can be undone later.
     *
     * @param mutableSolutionState never null; exposes all possible mutative operations on the variables.
     *        Remembers those mutative operations and can replay them in reverse order
     *        when the solver needs to undo the move.
     */
    void run(MutableSolutionState<Solution_> mutableSolutionState);

    /**
     * Called before a move is evaluated to decide whether the move can be done and evaluated.
     * Generally not required.
     * Move Streams are expected to only generate doable moves.
     * Exists to support compatibility with the old Move Selector API.
     * <p>
     * A Move is not doable if:
     * <ul>
     * <li>Either doing it would change nothing in the {@link PlanningSolution}.</li>
     * <li>Either it's simply not possible to do (for example due to built-in hard constraints).</li>
     * </ul>
     * <p>
     * It is recommended to keep this method implementation simple: do not use it in an attempt to satisfy normal
     * hard and soft constraints.
     * <p>
     * Although you could also filter out non-doable moves in for example the {@link MoveSelector}
     * or {@link MoveListFactory}, this is not needed as the {@link Solver} will do it for you.
     *
     * @param solutionState never null; exposes all possible read operations on the variables.
     * @return true if the move achieves a change in the solution and the move is possible to do on the solution.
     */
    default boolean isMoveDoable(SolutionState<Solution_> solutionState) {
        return true;
    }

    /**
     * Rebases a move from an origin {@link ScoreDirector} to another destination {@link ScoreDirector}
     * which is usually on another {@link Thread}.
     * It is necessary for multi-threaded solving to function.
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
     * translates each one to the destination {@link ScoreDirector} with {@link SolutionState#rebase(Object)}
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
     * @param solutionState never null; exposes all possible read operations on the variables.
     * @return never null, a new move that does the same change as this move on another solution instance
     */
    Move<Solution_> rebase(SolutionState<Solution_> solutionState);

    /**
     * Returns all planning entities that are being changed by this move.
     * Required for entity tabu.
     * <p>
     * This method is only called after {@link #run(MutableSolutionState)}, which might affect the return values.
     * <p>
     * Duplicate entries in the returned {@link Collection} are best avoided.
     * The returned {@link Collection} is recommended to be in a stable order.
     * For example: use {@link List} or {@link LinkedHashSet}, but not {@link HashSet}.
     *
     * @return never null
     */
    default Collection<?> getPlanningEntities() {
        throw new UnsupportedOperationException("The move (" + this + ") does not support tabu search.");
    }

    /**
     * Returns all planning values that entities are being assigned to by this move.
     * Required for value tabu.
     * <p>
     * This method is only called after {@link #run(MutableSolutionState)}, which might affect the return values.
     * <p>
     * Duplicate entries in the returned {@link Collection} are best avoided.
     * The returned {@link Collection} is recommended to be in a stable order.
     * For example: use {@link List} or {@link LinkedHashSet}, but not {@link HashSet}.
     *
     * @return never null
     */
    default Collection<?> getPlanningValues() {
        throw new UnsupportedOperationException("The move (" + this + ") does not support tabu search.");
    }

    /**
     * Describes the move type for statistical purposes.
     * For example "ChangeMove(Process.computer)".
     * <p>
     * The format is not formalized.
     * Never parse the {@link String} returned by this method.
     *
     * @return never null
     */
    default String getMoveTypeDescription() {
        return getClass().getSimpleName();
    }

    /**
     * The solver will make sure to only call this when the move is actually printed out during debug logging.
     *
     * @return A description of the move, ideally including the state of the planning entities being changed.
     */
    String toString();

}
