package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.RecommendedFit;

public record DefaultRecommendedFit<Result_, Score_ extends Score<Score_>>(long index, Result_ result,
        ScoreAnalysis<Score_> scoreAnalysisDiff)
        implements
            RecommendedFit<Result_, Score_>,
            Comparable<DefaultRecommendedFit<Result_, Score_>> {

    @Override
    public int compareTo(DefaultRecommendedFit<Result_, Score_> other) {
        int scoreComparison = scoreAnalysisDiff.score().compareTo(other.scoreAnalysisDiff.score());
        if (scoreComparison != 0) {
            return -scoreComparison; // Better scores first.
        }
        // Otherwise maintain insertion order.
        return Long.compareUnsigned(index, other.index); // Unsigned == many more positive values.
    }

}
