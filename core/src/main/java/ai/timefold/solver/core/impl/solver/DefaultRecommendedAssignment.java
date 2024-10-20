package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record DefaultRecommendedAssignment<Proposition_, Score_ extends Score<Score_>>(long index,
        @Nullable Proposition_ proposition, @NonNull ScoreAnalysis<Score_> scoreAnalysisDiff)
        implements
            RecommendedAssignment<Proposition_, Score_>,
            Comparable<DefaultRecommendedAssignment<Proposition_, Score_>> {

    @Override
    public int compareTo(DefaultRecommendedAssignment<Proposition_, Score_> other) {
        int scoreComparison = scoreAnalysisDiff.score().compareTo(other.scoreAnalysisDiff.score());
        if (scoreComparison != 0) {
            return -scoreComparison; // Better scores first.
        }
        // Otherwise maintain insertion order.
        return Long.compareUnsigned(index, other.index); // Unsigned == many more positive values.
    }

}
