package ai.timefold.solver.core.api.solver;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
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
 * @deprecated Prefer {@link RecommendedAssignment} instead.
 */
@Deprecated(forRemoval = true, since = "1.15.0")
public interface RecommendedFit<Proposition_, Score_ extends Score<Score_>>
        extends RecommendedAssignment<Proposition_, Score_> {

}
