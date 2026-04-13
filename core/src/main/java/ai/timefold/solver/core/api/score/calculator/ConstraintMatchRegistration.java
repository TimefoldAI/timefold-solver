package ai.timefold.solver.core.api.score.calculator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;

import org.jspecify.annotations.NullMarked;

/**
 * A registration for a constraint match, which can be cancelled.
 * <p>
 * This is used by {@link IncrementalScoreCalculator} implementations to register constraint matches,
 * which are later used for score justification and explanation.
 * <p>
 * No two instances should ever be equal;
 * if a constraint match is registered twice, two different instances will be counted.
 *
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public interface ConstraintMatchRegistration<Score_ extends Score<Score_>> {

    ConstraintRef constraintRef();

    Score_ score();

    /**
     * The justification of the match, which will be used in {@link ScoreAnalysis}.
     *
     * @return the justification of the match; the specific type depends on the user.
     */
    ConstraintJustification justification();

    /**
     * Cancels the registration of this constraint match.
     * Once canceled, the constraint match will no longer be counted for the purposes of score analysis.
     * Once {@link IncrementalScoreCalculator#resetWorkingSolution(Object)} is called,
     * all constraint matches registered with this instances' {@link ConstraintMatchRegistry} are automatically canceled
     * and the user can throw away the references.
     */
    void cancel();

}
