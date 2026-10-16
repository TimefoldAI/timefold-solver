package ai.timefold.solver.core.api.solver.termination;

import java.util.OptionalInt;

import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Identifies one termination in the termination configuration.
 * {@link SolverRunSummary#termination()} describes which id it returns.
 * <p>
 * The termination configuration is a tree of {@link TerminationConfig} elements.
 * Each element has its own terminations, such as {@code secondsSpentLimit} or {@code bestScoreLimit},
 * and can contain more elements in {@link TerminationConfig#getTerminationConfigList()}.
 * An id names either one of these terminations, or an element;
 * the {@link #type()} of an element is {@link TerminationType#AND} or {@link TerminationType#OR}.
 * All {@code *SpentLimit} properties of one element, such as {@code secondsSpentLimit} and {@code minutesSpentLimit},
 * are one {@link TerminationType#SPENT_LIMIT} termination.
 * <p>
 * Example, with each id as {@code (type, parent, ordinal)}:
 *
 * <pre>{@code
 * <termination>                                                <!-- R = (OR, none, 0) -->
 *   <secondsSpentLimit>30</secondsSpentLimit>                  <!-- (SPENT_LIMIT, R, 0) -->
 *   <minutesSpentLimit>1</minutesSpentLimit>                   <!-- the same termination as secondsSpentLimit -->
 *   <termination>                                              <!-- A = (OR, R, 0) -->
 *     <bestScoreLimit>0</bestScoreLimit>                       <!-- (BEST_SCORE_LIMIT, A, 0) -->
 *   </termination>
 *   <termination>                                              <!-- B = (AND, R, 1) -->
 *     <terminationCompositionStyle>AND</terminationCompositionStyle>
 *     <stepCountLimit>100</stepCountLimit>                     <!-- (STEP_COUNT_LIMIT, B, 0) -->
 *     <unimprovedStepCountLimit>10</unimprovedStepCountLimit>  <!-- (UNIMPROVED_STEP_COUNT_LIMIT, B, 0) -->
 *   </termination>
 * </termination>
 * }</pre>
 *
 * <p>
 * Two ids are equal if all their parts are equal,
 * so solvers built from the same configuration give equal ids.
 */
@NullMarked
public interface TerminationId {

    /**
     * @return {@link TerminationType#AND} or {@link TerminationType#OR} if this id names an element;
     *         else the type of the termination
     */
    TerminationType type();

    /**
     * @return the element that contains this termination or element; null for the root element
     */
    @Nullable
    TerminationId parent();

    /**
     * One element has no more than one termination of each type,
     * so {@code (type, parent, ordinal)} is unique in one termination configuration.
     *
     * @return for an element, its index in {@link TerminationConfig#getTerminationConfigList()} of its parent;
     *         0 for the root element and for a termination
     */
    int ordinal();

    /**
     * @return the index in {@link SolverConfig#getPhaseConfigList()} of the phase that has this termination configured;
     *         empty if the termination is configured on the solver
     */
    OptionalInt phaseIndex();

}
