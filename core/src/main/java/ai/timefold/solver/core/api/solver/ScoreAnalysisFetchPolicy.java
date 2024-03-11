package ai.timefold.solver.core.api.solver;

import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;

/**
 * Determines the depth of {@link SolutionManager#analyze(Object) score analysis}.
 * If unsure, pick {@link #FETCH_ALL}.
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
     * does not include {@link ConstraintAnalysis#matches() match analysis}.
     * This is useful for performance reasons when the match analysis is not needed.
     */
    FETCH_SHALLOW

}
