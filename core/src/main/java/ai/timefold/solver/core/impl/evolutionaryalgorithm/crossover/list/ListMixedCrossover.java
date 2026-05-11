package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverResult;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ListMixedCrossover<Solution_, Score_ extends Score<Score_>> implements CrossoverStrategy<Solution_, Score_> {

    private final CrossoverStrategy<Solution_, Score_> firstStrategy;
    private final CrossoverStrategy<Solution_, Score_> secondStrategy;

    public ListMixedCrossover(CrossoverStrategy<Solution_, Score_> firstStrategy,
            CrossoverStrategy<Solution_, Score_> secondStrategy) {
        this.firstStrategy = Objects.requireNonNull(firstStrategy);
        this.secondStrategy = Objects.requireNonNull(secondStrategy);
    }

    @Override
    public CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context) {
        if (context.phaseScope().getWorkingRandom().nextBoolean()) {
            return firstStrategy.apply(context);
        } else {
            return secondStrategy.apply(context);
        }
    }
}
