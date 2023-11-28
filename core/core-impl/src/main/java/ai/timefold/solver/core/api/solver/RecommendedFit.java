package ai.timefold.solver.core.api.solver;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;

/**
 * Represents the result of the Recommended Fit API,
 * see {@link SolutionManager#recommendFit(Object, Object, Function)}.
 *
 * <p>
 * In order to be fully serializable to JSON, propositions must be fully serializable to JSON.
 * For deserialization from JSON, the user needs to provide the deserializer themselves.
 * This is due to the fact that, once the proposition is received over the wire,
 * we no longer know which type was used.
 * The user has all of that information in their domain model,
 * and so they are the correct party to provide the deserializer.
 * See also {@link ScoreAnalysis} Javadoc for additional notes on serializing and deserializing that type.
 *
 * @param <Proposition_> the generic type of the proposition as returned by the proposition function
 * @param <Score_> the generic type of the score
 */
public interface RecommendedFit<Proposition_, Score_ extends Score<Score_>> {

    /**
     * Returns the proposition as returned by the proposition function.
     * This is the actual assignment recommended to the user.
     *
     * @return null if proposition function required null
     */
    Proposition_ proposition();

    /**
     * Difference between the original score and the score of the solution with the recommendation applied.
     *
     * <p>
     * If {@link SolutionManager#recommendFit(Object, Object, Function, ScoreAnalysisFetchPolicy)} was called with
     * {@link ScoreAnalysisFetchPolicy#FETCH_ALL},
     * the analysis will include {@link MatchAnalysis constraint matches}
     * inside its {@link ConstraintAnalysis constraint analysis};
     * otherwise it will not.
     *
     * @return never null; {@code fittedScoreAnalysis - originalScoreAnalysis} as defined by
     *         {@link ScoreAnalysis#diff(ScoreAnalysis)}
     */
    ScoreAnalysis<Score_> scoreAnalysisDiff();

}
