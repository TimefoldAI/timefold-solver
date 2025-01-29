package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;
import ai.timefold.solver.core.api.solver.SolverJobBuilder.FirstInitializedSolutionConsumer;
import ai.timefold.solver.core.impl.constructionheuristic.ConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.localsearch.LocalSearchPhase;
import ai.timefold.solver.core.impl.phase.custom.CustomPhase;

import org.jspecify.annotations.NullMarked;

/**
 * Describes a phase that can be used to initialize a solution.
 * {@link ConstructionHeuristicPhase} is automatically an initializing phase.
 * {@link CustomPhase} can be an initializing phase, if it comes before the first {@link LocalSearchPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface PossiblyInitializingPhase<Solution_> extends Phase<Solution_> {

    /**
     * Check if a phase should trigger the first initialized solution event.
     * The first initialized solution immediately precedes the first {@link LocalSearchPhase}.
     * 
     * @return true if the phase is the final phase before the first local search phase.
     * @see SolverJobBuilder#withFirstInitializedSolutionConsumer(FirstInitializedSolutionConsumer)
     */
    boolean isLastInitializingPhase();

    /**
     * The status with which the phase terminated.
     */
    TerminationStatus getTerminationStatus();

    /**
     * The status with which the phase terminated.
     * 
     * @param terminated If the phase terminated.
     * @param early If the phase terminated early without completing all steps.
     *        If true, this signifies a solution that is not fully initialized.
     * @param stepCount The number of steps completed.
     */
    record TerminationStatus(boolean terminated, boolean early, int stepCount) {

        public static TerminationStatus NOT_TERMINATED = new TerminationStatus(false, false, -1);

        public static TerminationStatus regular(int stepCount) {
            return new TerminationStatus(true, false, stepCount);
        }

        public static TerminationStatus early(int stepCount) {
            return new TerminationStatus(true, true, stepCount);
        }

    }

}
