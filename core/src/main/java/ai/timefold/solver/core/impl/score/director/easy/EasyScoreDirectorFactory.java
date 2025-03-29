package ai.timefold.solver.core.impl.score.director.easy;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;

/**
 * Easy implementation of {@link ScoreDirectorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see EasyScoreDirector
 * @see ScoreDirectorFactory
 */
public final class EasyScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirectorFactory<Solution_, Score_, EasyScoreDirectorFactory<Solution_, Score_>> {

    public static <Solution_, Score_ extends Score<Score_>> EasyScoreDirectorFactory<Solution_, Score_>
            buildScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor, ScoreDirectorFactoryConfig config) {
        var easyScoreCalculatorClass = config.getEasyScoreCalculatorClass();
        if (easyScoreCalculatorClass == null || !EasyScoreCalculator.class.isAssignableFrom(easyScoreCalculatorClass)) {
            throw new IllegalArgumentException(
                    "The easyScoreCalculatorClass (%s) does not implement %s."
                            .formatted(config.getEasyScoreCalculatorClass(), EasyScoreCalculator.class.getSimpleName()));
        }
        EasyScoreCalculator<Solution_, Score_> easyScoreCalculator =
                ConfigUtils.newInstance(config, "easyScoreCalculatorClass", easyScoreCalculatorClass);
        ConfigUtils.applyCustomProperties(easyScoreCalculator, "easyScoreCalculatorClass",
                config.getEasyScoreCalculatorCustomProperties(), "easyScoreCalculatorCustomProperties");
        return new EasyScoreDirectorFactory<>(solutionDescriptor, easyScoreCalculator);
    }

    private final EasyScoreCalculator<Solution_, Score_> easyScoreCalculator;

    public EasyScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            EasyScoreCalculator<Solution_, Score_> easyScoreCalculator) {
        super(solutionDescriptor);
        this.easyScoreCalculator = easyScoreCalculator;
    }

    @Override
    public EasyScoreDirector.Builder<Solution_, Score_> createScoreDirectorBuilder() {
        return new EasyScoreDirector.Builder<>(this)
                .withEasyScoreCalculator(easyScoreCalculator);
    }

    @Override
    public AbstractScoreDirector<Solution_, Score_, ?> buildScoreDirector() {
        return this.createScoreDirectorBuilder().build();
    }

}
