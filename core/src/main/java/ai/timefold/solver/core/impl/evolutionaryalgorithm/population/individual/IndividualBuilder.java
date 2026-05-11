package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@FunctionalInterface
public interface IndividualBuilder<Solution_, Score_ extends Score<Score_>> {

    Individual<Solution_, Score_> build(Solution_ solution, InnerScore<Score_> score,
            @Nullable InnerScore<Score_> firstParentScore, @Nullable InnerScore<Score_> secondParentScore,
            InnerScoreDirector<Solution_, Score_> scoreDirector);
}
