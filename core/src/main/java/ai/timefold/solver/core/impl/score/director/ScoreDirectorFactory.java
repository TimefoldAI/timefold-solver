package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector.AbstractScoreDirectorBuilder;
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

    <Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>, Builder_ extends AbstractScoreDirectorBuilder<Solution_, Score_, Factory_, Builder_>>
            AbstractScoreDirectorBuilder<Solution_, Score_, Factory_, Builder_>
            createScoreDirectorBuilder();

    default <Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>>
            AbstractScoreDirector<Solution_, Score_, Factory_> buildScoreDirector() {
        AbstractScoreDirectorBuilder<Solution_, Score_, Factory_, ?> builder = createScoreDirectorBuilder();
        return builder.build();
    }

    /**
     * @return never null
     */
    EnvironmentMode getEnvironmentMode();

    /**
     * @return never null
     */
    InitializingScoreTrend getInitializingScoreTrend();

}
