package ai.timefold.solver.core.api.score.calculator;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Score_>
 * @see AnalyzableIncrementalScoreCalculator Adding explainability to incremental score calculator.
 */
@NullMarked
public interface ConstraintMatchRegistry<Score_ extends Score<Score_>> {

    /**
     *
     * @param constraintRef identification of the constraint; obtain from {@link ConstraintRef#of(String)}.
     * @param score full impact of the match;
     *        Any {@link ConstraintWeightOverrides} will be ignored.
     * @param justification the justification of the match, which will be used in {@link ScoreAnalysis};
     *        use {@link #registerConstraintMatch(ConstraintRef, Score_, Object...)}
     *        if you prefer to not provide a specific justification type.
     * @throws IllegalStateException if constraint matching is not enabled
     * @return the handler to cancel the match later if needed
     */
    ConstraintMatchRegistration<Score_> registerConstraintMatch(ConstraintRef constraintRef, Score_ score,
            ConstraintJustification justification);

    /**
     *
     * @param constraintRef identification of the constraint; obtain from {@link ConstraintRef#of(String)}.
     * @param score full impact of the match;
     *        Any {@link ConstraintWeightOverrides} will be ignored.
     * @param justifications objects justifying the match, which will be used in {@link ScoreAnalysis};
     *        these objects will be wrapped by a default internal {@link ConstraintJustification} implementation.
     * @throws IllegalStateException if constraint matching is not enabled
     * @return the handler to cancel the match later if needed
     */
    default ConstraintMatchRegistration<Score_> registerConstraintMatch(ConstraintRef constraintRef, Score_ score,
            Object... justifications) {
        return registerConstraintMatch(constraintRef, score, DefaultConstraintJustification.of(score, justifications));
    }

    /**
     * @return the total score of all registered and non-canceled matches
     */
    Score_ totalScore();

}
