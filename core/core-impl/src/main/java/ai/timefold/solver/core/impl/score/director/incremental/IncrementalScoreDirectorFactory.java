package ai.timefold.solver.core.impl.score.director.incremental;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;

/**
 * Incremental implementation of {@link ScoreDirectorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see IncrementalScoreDirector
 * @see ScoreDirectorFactory
 */
public class IncrementalScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirectorFactory<Solution_, Score_> {

    private final Supplier<IncrementalScoreCalculator<Solution_, Score_>> incrementalScoreCalculatorSupplier;

    public IncrementalScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            Supplier<IncrementalScoreCalculator<Solution_, Score_>> incrementalScoreCalculatorSupplier) {
        super(solutionDescriptor);
        this.incrementalScoreCalculatorSupplier = incrementalScoreCalculatorSupplier;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public IncrementalScoreDirector<Solution_, Score_> buildScoreDirector(
            boolean lookUpEnabled, boolean constraintMatchEnabledPreference) {
        return new IncrementalScoreDirector<>(this,
                lookUpEnabled, constraintMatchEnabledPreference, incrementalScoreCalculatorSupplier.get());
    }

}
