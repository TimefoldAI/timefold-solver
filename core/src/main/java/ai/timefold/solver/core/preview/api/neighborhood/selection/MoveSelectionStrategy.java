package ai.timefold.solver.core.preview.api.neighborhood.selection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses the {@link MoveSource} that gives the next move in a Local Search step.
 * Without a configured strategy, the solver uses {@link FixedWeightMoveSelectionStrategy},
 * which gives each {@link MoveProvider} the same chance.
 * <p>
 * The solver creates a new instance for each phase run, at the start of the phase,
 * and applies the custom properties then.
 * The class must have a public no-arg constructor.
 * The solver calls the instance only from the solver thread.
 * <p>
 * Call order: {@link #phaseStarted(PhaseContext)} once;
 * then for each step {@link #stepStarted(StepContext)},
 * followed by {@link #nextMoveSource()} for each move,
 * and {@link #moveSourceExhausted()} when the move source returned last has no more moves.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface MoveSelectionStrategy<Solution_> {

    void phaseStarted(PhaseContext<Solution_> context);

    /**
     * After this call, all move sources are available again.
     */
    void stepStarted(StepContext<Solution_> context);

    /**
     * The move source that {@link #nextMoveSource()} returned last has no more moves in this step.
     * The solver calls this either right after that move source was returned or right after its last move.
     * {@link #nextMoveSource()} must not return it again before the next {@link #stepStarted(StepContext)}.
     */
    void moveSourceExhausted();

    /**
     * The solver calls this only while at least one move source is available.
     * The solver gets the moves of a move source only when this method first returns it in a step,
     * so a move source can be exhausted right after it is returned.
     * If the step ends before any move was accepted, the solver takes the best move that it evaluated.
     * For random decisions, use only {@link PhaseContext#random()}, so that runs are reproducible.
     *
     * @return an available move source from {@link PhaseContext#moveSources()}, or null to end the step;
     *         an exhausted or unknown move source makes the solver fail
     */
    @Nullable
    MoveSource<Solution_> nextMoveSource();

}
