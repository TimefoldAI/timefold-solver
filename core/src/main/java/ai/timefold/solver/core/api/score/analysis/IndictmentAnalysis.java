package ai.timefold.solver.core.api.score.analysis;

import java.util.List;

import ai.timefold.solver.core.api.score.Score;

/**
 * Note: {@link ScoreAnalysis} is exclusive to Timefold Solver Enterprise Edition.
 *
 * @param <Score_>
 * @see ScoreAnalysis Description of score analysis and the purpose of this class.
 */
public interface IndictmentAnalysis<Score_ extends Score<Score_>> {
    /**
     * The object that was indicted by constraints.
     * 
     * @return the indicted object
     */
    Object indictee();

    /**
     * The sum of score impacts of each constraint match the {@link #indictee()}
     * is involved with.
     * 
     * @return the total score impact of the indicted object
     */
    Score_ score();

    /**
     * A list of {@link MatchAnalysis} for each constraint match the
     * {@link #indictee()} is involved with, sorted by absolute score impact
     * 
     * @return a list of matches the indictee is involved with, sorted by absolute score impact
     */
    List<MatchAnalysis<Score_>> matches();
}
