package ai.timefold.solver.core.impl.score.director.easy;

import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryService;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorType;

public final class EasyScoreDirectorFactoryService<Solution_, Score_ extends Score<Score_>>
        implements ScoreDirectorFactoryService<Solution_, Score_> {

    @Override
    public ScoreDirectorType getSupportedScoreDirectorType() {
        return ScoreDirectorType.EASY;
    }

    @Override
    public Supplier<AbstractScoreDirectorFactory<Solution_, Score_>> buildScoreDirectorFactory(ClassLoader classLoader,
            SolutionDescriptor<Solution_> solutionDescriptor, ScoreDirectorFactoryConfig config,
            EnvironmentMode environmentMode) {
        if (config.getEasyScoreCalculatorClass() != null) {
            if (!EasyScoreCalculator.class.isAssignableFrom(config.getEasyScoreCalculatorClass())) {
                throw new IllegalArgumentException(
                        "The easyScoreCalculatorClass (" + config.getEasyScoreCalculatorClass()
                                + ") does not implement " + EasyScoreCalculator.class.getSimpleName() + ".");
            }
            return () -> {
                EasyScoreCalculator<Solution_, Score_> easyScoreCalculator = ConfigUtils.newInstance(config,
                        "easyScoreCalculatorClass", config.getEasyScoreCalculatorClass());
                ConfigUtils.applyCustomProperties(easyScoreCalculator, "easyScoreCalculatorClass",
                        config.getEasyScoreCalculatorCustomProperties(), "easyScoreCalculatorCustomProperties");
                return new EasyScoreDirectorFactory<>(solutionDescriptor, easyScoreCalculator);
            };
        } else {
            if (config.getEasyScoreCalculatorCustomProperties() != null) {
                throw new IllegalStateException(
                        "If there is no easyScoreCalculatorClass (" + config.getEasyScoreCalculatorClass()
                                + "), then there can be no easyScoreCalculatorCustomProperties ("
                                + config.getEasyScoreCalculatorCustomProperties() + ") either.");
            }
            return null;
        }
    }
}
