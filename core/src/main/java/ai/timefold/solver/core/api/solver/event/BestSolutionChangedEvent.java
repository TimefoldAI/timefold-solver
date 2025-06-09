package ai.timefold.solver.core.api.solver.event;

import java.util.EventObject;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NonNull;

/**
 * Delivered when the {@link PlanningSolution best solution} changes during solving.
 * Delivered in the solver thread (which is the thread that calls {@link Solver#solve}).
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
// TODO In Solver 2.0, maybe convert this to an interface.
public class BestSolutionChangedEvent<Solution_> extends EventObject {

    private final Solver<Solution_> solver;
    private final long timeMillisSpent;
    private final Solution_ newBestSolution;
    private final Score newBestScore;
    private final boolean isNewBestSolutionInitialized;

    /**
     * @param timeMillisSpent {@code >= 0L}
     * @deprecated Users should not manually construct instances of this event.
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public BestSolutionChangedEvent(@NonNull Solver<Solution_> solver, long timeMillisSpent,
            @NonNull Solution_ newBestSolution, @NonNull Score newBestScore) {
        this(solver, timeMillisSpent, newBestSolution, newBestScore, true);
    }

    /**
     * @param timeMillisSpent {@code >= 0L}
     * @deprecated Users should not manually construct instances of this event.
     */
    @Deprecated(forRemoval = true, since = "1.23.0")
    public BestSolutionChangedEvent(@NonNull Solver<Solution_> solver, long timeMillisSpent,
            @NonNull Solution_ newBestSolution, @NonNull Score newBestScore,
            boolean isNewBestSolutionInitialized) {
        super(solver);
        this.solver = solver;
        this.timeMillisSpent = timeMillisSpent;
        this.newBestSolution = newBestSolution;
        this.newBestScore = newBestScore;
        this.isNewBestSolutionInitialized = isNewBestSolutionInitialized;
    }

    /**
     * @return {@code >= 0}, the amount of millis spent since the {@link Solver} started
     *         until {@link #getNewBestSolution()} was found
     */
    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    /**
     * Note that:
     * <ul>
     * <li>In real-time planning, not all {@link ProblemChange}s might be processed:
     * check {@link #isEveryProblemFactChangeProcessed()}.</li>
     * <li>this {@link PlanningSolution} might be uninitialized: check {@link #isNewBestSolutionInitialized()}.</li>
     * <li>this {@link PlanningSolution} might be infeasible: check {@link Score#isFeasible()}.</li>
     * </ul>
     *
     */
    public @NonNull Solution_ getNewBestSolution() {
        return newBestSolution;
    }

    /**
     * Returns the {@link Score} of the {@link #getNewBestSolution()}.
     * <p>
     * This is useful for generic code, which doesn't know the type of the {@link PlanningSolution}
     * to retrieve the {@link Score} from the {@link #getNewBestSolution()} easily.
     */
    public @NonNull Score getNewBestScore() {
        return newBestScore;
    }

    /**
     * @return True if {@link #getNewBestSolution()} is initialized.
     */
    public boolean isNewBestSolutionInitialized() {
        return isNewBestSolutionInitialized;
    }

    /**
     * This method is deprecated.
     *
     * @deprecated Prefer {@link #isEveryProblemChangeProcessed}.
     * @return As defined by {@link Solver#isEveryProblemFactChangeProcessed()}
     * @see Solver#isEveryProblemFactChangeProcessed()
     */
    @Deprecated(forRemoval = true)
    public boolean isEveryProblemFactChangeProcessed() {
        return solver.isEveryProblemFactChangeProcessed();
    }

    /**
     * @return As defined by {@link Solver#isEveryProblemChangeProcessed()}
     * @see Solver#isEveryProblemChangeProcessed()
     */
    public boolean isEveryProblemChangeProcessed() {
        return solver.isEveryProblemChangeProcessed();
    }

}
