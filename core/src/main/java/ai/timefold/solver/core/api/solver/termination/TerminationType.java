package ai.timefold.solver.core.api.solver.termination;

import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * The type of a {@link TerminationId}.
 * See {@link TerminationId} for the id model.
 * Custom terminations are not supported by the solver,
 * and therefore this type does not express that possibility;
 * if a new termination is added in the future,
 * this enum will be extended.
 */
public enum TerminationType {

    /**
     * Built from {@link TerminationConfig#getSpentLimit()} and all other {@code *SpentLimit} settings.
     */
    SPENT_LIMIT,
    /**
     * Built from {@link TerminationConfig#getUnimprovedSpentLimit()} and all other {@code unimproved*SpentLimit} settings,
     * when {@link TerminationConfig#getUnimprovedScoreDifferenceThreshold()} is not set.
     */
    UNIMPROVED_SPENT_LIMIT,
    /**
     * Built from {@link TerminationConfig#getUnimprovedScoreDifferenceThreshold()}
     * together with the {@code unimproved*SpentLimit} settings.
     */
    UNIMPROVED_SCORE_DIFFERENCE_THRESHOLD,
    /**
     * Built from {@link TerminationConfig#getDiminishedReturnsConfig()}.
     */
    DIMINISHED_RETURNS,
    /**
     * Built from {@link TerminationConfig#getBestScoreLimit()}.
     */
    BEST_SCORE_LIMIT,
    /**
     * Built from {@link TerminationConfig#getBestScoreFeasible()}.
     */
    BEST_SCORE_FEASIBLE,
    /**
     * Built from {@link TerminationConfig#getStepCountLimit()}.
     */
    STEP_COUNT_LIMIT,
    /**
     * Built from {@link TerminationConfig#getScoreCalculationCountLimit()}.
     */
    SCORE_CALCULATION_COUNT_LIMIT,
    /**
     * Built from {@link TerminationConfig#getUnimprovedStepCountLimit()}.
     */
    UNIMPROVED_STEP_COUNT_LIMIT,
    /**
     * Built from {@link TerminationConfig#getMoveCountLimit()}.
     */
    MOVE_COUNT_LIMIT,
    /**
     * A {@link TerminationConfig} element with {@link TerminationCompositionStyle#AND}.
     * See {@link SolverRunSummary#termination()} for when it is reported.
     */
    AND,
    /**
     * A {@link TerminationConfig} element with {@link TerminationCompositionStyle#OR}, the default.
     * It appears only as {@link TerminationId#parent()}; see {@link SolverRunSummary#termination()}.
     */
    OR

}
