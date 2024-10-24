package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedFit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @deprecated Prefer {@link DefaultRecommendedAssignment} instead.
 */
@Deprecated(forRemoval = true, since = "1.15.0")
public record DefaultRecommendedFit<Proposition_, Score_ extends Score<Score_>>(long index, @Nullable Proposition_ proposition,
        @NonNull ScoreAnalysis<Score_> scoreAnalysisDiff)
        implements
            RecommendedFit<Proposition_, Score_>,
            Comparable<DefaultRecommendedFit<Proposition_, Score_>> {

    @Override
    public int compareTo(DefaultRecommendedFit<Proposition_, Score_> other) {
        int scoreComparison = scoreAnalysisDiff.score().compareTo(other.scoreAnalysisDiff.score());
        if (scoreComparison != 0) {
            return -scoreComparison; // Better scores first.
        }
        // Otherwise maintain insertion order.
        return Long.compareUnsigned(index, other.index); // Unsigned == many more positive values.
    }

}
