package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 */
public interface ScoreDirectorFactory<Solution_, Score_ extends Score<Score_>> {

    /**
     * @return never null
     */
    SolutionDescriptor<Solution_> getSolutionDescriptor();

    /**
     * @return never null
     */
    ScoreDefinition<Score_> getScoreDefinition();

    AbstractScoreDirector.AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder();

    default AbstractScoreDirector<Solution_, Score_, ?> buildScoreDirector() {
        return createScoreDirectorBuilder().build();
    }

    /**
     * @return never null
     */
    InitializingScoreTrend getInitializingScoreTrend();

    /**
     * Asserts that if the {@link Score} is calculated for the parameter solution,
     * it would be equal to the score of that parameter.
     *
     * @param solution never null
     * @see InnerScoreDirector#assertWorkingScoreFromScratch(InnerScore, Object)
     */
    void assertScoreFromScratch(Solution_ solution);

    default boolean supportsConstraintMatching() {
        return false;
    }

}
