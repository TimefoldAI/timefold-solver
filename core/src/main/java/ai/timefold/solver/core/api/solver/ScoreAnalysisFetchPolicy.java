package ai.timefold.solver.core.api.solver;

import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;

/**
 * Determines the depth of {@link SolutionManager#analyze(Object) score analysis}.
 * If unsure, pick {@link #FETCH_MATCH_COUNT}.
 *
 */
public enum ScoreAnalysisFetchPolicy {

    /**
     * {@link ScoreAnalysis} is fully initialized.
     * All included {@link ConstraintAnalysis} objects include full {@link ConstraintAnalysis#matches() match analysis}.
     */
    FETCH_ALL,
    /**
     * {@link ConstraintAnalysis} included in {@link ScoreAnalysis}
     * provides neither {@link ConstraintAnalysis#matches() match analysis}
     * nor {@link ConstraintAnalysis#matchCount() match count}.
     * This is useful for performance reasons when the match analysis is not needed.
     */
    FETCH_SHALLOW,
    /**
     * {@link ConstraintAnalysis} included in {@link ScoreAnalysis}
     * does not provide {@link ConstraintAnalysis#matches() match analysis},
     * but does provide {@link ConstraintAnalysis#matchCount() match count}.
     * This is useful when there are too many matches to send over the wire
     * or meaningfully present to users.
     */
    FETCH_MATCH_COUNT

}
