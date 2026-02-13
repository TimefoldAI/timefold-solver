package ai.timefold.solver.core.api.solver.event;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NullMarked;

/**
 * Delivered when the {@link PlanningSolution best solution} changes during solving.
 * Delivered in the solver thread (which is the thread that calls {@link Solver#solve}).
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface BestSolutionChangedEvent<Solution_> {

    /**
     * @return {@code >= 0}, the amount of millis spent since the {@link Solver} started
     *         until {@link #getNewBestSolution()} was found
     */
    long getTimeMillisSpent();

    /**
     * @return A {@link EventProducerId} identifying what generated the event
     */
    EventProducerId getProducerId();

    /**
     * Note that:
     * <ul>
     * <li>In real-time planning, not all {@link ProblemChange}s might be processed:
     * check {@link #isEveryProblemChangeProcessed()}.</li>
     * <li>this {@link PlanningSolution} might be uninitialized: check {@link #isNewBestSolutionInitialized()}.</li>
     * <li>this {@link PlanningSolution} might be infeasible: check {@link Score#isFeasible()}.</li>
     * </ul>
     *
     */
    Solution_ getNewBestSolution();

    /**
     * Returns the {@link Score} of the {@link #getNewBestSolution()}.
     * <p>
     * This is useful for generic code, which doesn't know the type of the {@link PlanningSolution}
     * to retrieve the {@link Score} from the {@link #getNewBestSolution()} easily.
     */
    Score getNewBestScore();

    /**
     * @return True if {@link #getNewBestSolution()} is initialized.
     */
    boolean isNewBestSolutionInitialized();

    /**
     * @return As defined by {@link Solver#isEveryProblemChangeProcessed()}
     * @see Solver#isEveryProblemChangeProcessed()
     */
    boolean isEveryProblemChangeProcessed();

}
