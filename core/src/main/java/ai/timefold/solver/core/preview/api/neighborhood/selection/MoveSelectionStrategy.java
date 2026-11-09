package ai.timefold.solver.core.preview.api.neighborhood.selection;

import java.util.random.RandomGenerator;

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
 * followed by {@link #nextMoveSource(RandomGenerator)} and {@link #moveSourceExhausted(MoveSource)} in any mix.
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
     * The move source has no more moves in this step.
     * {@link #nextMoveSource(RandomGenerator)} must not return it again before the next {@link #stepStarted(StepContext)}.
     */
    void moveSourceExhausted(MoveSource<Solution_> moveSource);

    /**
     * The solver calls this only while at least one move source is available.
     * If the step ends before any move was accepted, the solver takes the best move that it evaluated.
     *
     * @param random use only this for random decisions, so that runs are reproducible
     * @return an available move source from {@link PhaseContext#moveSources()}, or null to end the step;
     *         an exhausted or unknown move source makes the solver fail,
     *         and so does null before the first move of a step
     */
    @Nullable
    MoveSource<Solution_> nextMoveSource(RandomGenerator random);

}
