package ai.timefold.solver.core.impl.score.director;

import static ai.timefold.solver.core.impl.score.director.ScoreDirectorType.CONSTRAINT_STREAMS;
import static ai.timefold.solver.core.impl.score.director.ScoreDirectorType.EASY;
import static ai.timefold.solver.core.impl.score.director.ScoreDirectorType.INCREMENTAL;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class ScoreDirectorFactoryFactory<Solution_, Score_ extends Score<Score_>> {

    private final ScoreDirectorFactoryConfig config;

    public ScoreDirectorFactoryFactory(ScoreDirectorFactoryConfig config) {
        this.config = config;
    }

    public InnerScoreDirectorFactory<Solution_, Score_> buildScoreDirectorFactory(ClassLoader classLoader,
            EnvironmentMode environmentMode, SolutionDescriptor<Solution_> solutionDescriptor) {
        AbstractScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory =
                decideMultipleScoreDirectorFactories(classLoader, solutionDescriptor, environmentMode);
        if (config.getAssertionScoreDirectorFactory() != null) {
            if (config.getAssertionScoreDirectorFactory().getAssertionScoreDirectorFactory() != null) {
                throw new IllegalArgumentException("A assertionScoreDirectorFactory ("
                        + config.getAssertionScoreDirectorFactory() + ") cannot have a non-null assertionScoreDirectorFactory ("
                        + config.getAssertionScoreDirectorFactory().getAssertionScoreDirectorFactory() + ").");
            }
            if (environmentMode.compareTo(EnvironmentMode.FAST_ASSERT) > 0) {
                throw new IllegalArgumentException("A non-null assertionScoreDirectorFactory ("
                        + config.getAssertionScoreDirectorFactory() + ") requires an environmentMode ("
                        + environmentMode + ") of " + EnvironmentMode.FAST_ASSERT + " or lower.");
            }
            ScoreDirectorFactoryFactory<Solution_, Score_> assertionScoreDirectorFactoryFactory =
                    new ScoreDirectorFactoryFactory<>(config.getAssertionScoreDirectorFactory());
            scoreDirectorFactory.setAssertionScoreDirectorFactory(assertionScoreDirectorFactoryFactory
                    .buildScoreDirectorFactory(classLoader, EnvironmentMode.NON_REPRODUCIBLE, solutionDescriptor));
        }
        scoreDirectorFactory.setInitializingScoreTrend(InitializingScoreTrend.parseTrend(
                config.getInitializingScoreTrend() == null ? InitializingScoreTrendLevel.ANY.name()
                        : config.getInitializingScoreTrend(),
                solutionDescriptor.getScoreDefinition().getLevelsSize()));
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            scoreDirectorFactory.setAssertClonedSolution(true);
        }
        if (environmentMode.isTracking()) {
            scoreDirectorFactory.setTrackingWorkingSolution(true);
        }
        return scoreDirectorFactory;
    }

    protected AbstractScoreDirectorFactory<Solution_, Score_> decideMultipleScoreDirectorFactories(
            ClassLoader classLoader, SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        // Load all known Score Director Factories via SPI.
        ServiceLoader<ScoreDirectorFactoryService> scoreDirectorFactoryServiceLoader =
                classLoader == null
                        ? ServiceLoader.load(ScoreDirectorFactoryService.class)
                        : ServiceLoader.load(ScoreDirectorFactoryService.class, classLoader);
        Map<ScoreDirectorType, Supplier<AbstractScoreDirectorFactory<Solution_, Score_>>> scoreDirectorFactorySupplierMap =
                new EnumMap<>(ScoreDirectorType.class);
        for (ScoreDirectorFactoryService<Solution_, Score_> service : scoreDirectorFactoryServiceLoader) {
            Supplier<AbstractScoreDirectorFactory<Solution_, Score_>> factory =
                    service.buildScoreDirectorFactory(classLoader, solutionDescriptor, config, environmentMode);
            if (factory != null) {
                scoreDirectorFactorySupplierMap.put(service.getSupportedScoreDirectorType(), factory);
            }
        }

        Supplier<AbstractScoreDirectorFactory<Solution_, Score_>> easyScoreDirectorFactorySupplier =
                scoreDirectorFactorySupplierMap.get(EASY);
        Supplier<AbstractScoreDirectorFactory<Solution_, Score_>> constraintStreamScoreDirectorFactorySupplier =
                scoreDirectorFactorySupplierMap.get(CONSTRAINT_STREAMS);
        Supplier<AbstractScoreDirectorFactory<Solution_, Score_>> incrementalScoreDirectorFactorySupplier =
                scoreDirectorFactorySupplierMap.get(INCREMENTAL);

        if (!ConfigUtils.isEmptyCollection(config.getScoreDrlList())) {
            throw new IllegalStateException("DRL constraints requested via scoreDrlList (" + config.getScoreDrlList()
                    + "), but this is no longer supported in Timefold Solver 0.9+.\n"
                    + "Maybe upgrade from scoreDRL to ConstraintStreams using this recipe: https://www.optaplanner.org/download/upgradeRecipe/drl-to-constraint-streams-migration.html\n"
                    + "Maybe use Timefold Solver 0.8 instead if you can't upgrade to ConstraintStreams now.");
        }
        // Every non-null supplier means that ServiceLoader successfully loaded and configured a score director factory.
        assertOnlyOneScoreDirectorFactory(easyScoreDirectorFactorySupplier,
                constraintStreamScoreDirectorFactorySupplier, incrementalScoreDirectorFactorySupplier);

        if (easyScoreDirectorFactorySupplier != null) {
            return easyScoreDirectorFactorySupplier.get();
        } else if (incrementalScoreDirectorFactorySupplier != null) {
            return incrementalScoreDirectorFactorySupplier.get();
        }
        if (constraintStreamScoreDirectorFactorySupplier != null) {
            return constraintStreamScoreDirectorFactorySupplier.get();
        } else if (config.getConstraintProviderClass() != null) {
            throw new IllegalStateException("Constraint Streams requested via constraintProviderClass (" +
                    config.getConstraintProviderClass() + ") but the supporting classes were not found on the classpath.\n"
                    + "Maybe include ai.timefold.solver:timefold-solver-constraint-streams dependency in your project?\n"
                    + "Maybe ensure your uberjar bundles META-INF/services from included JAR files?");
        }

        throw new IllegalArgumentException("The scoreDirectorFactory lacks configuration for "
                + "either constraintProviderClass, easyScoreCalculatorClass or incrementalScoreCalculatorClass.");
    }

    private void assertOnlyOneScoreDirectorFactory(
            Supplier<? extends ScoreDirectorFactory<Solution_>> easyScoreDirectorFactorySupplier,
            Supplier<? extends ScoreDirectorFactory<Solution_>> constraintStreamScoreDirectorFactorySupplier,
            Supplier<? extends ScoreDirectorFactory<Solution_>> incrementalScoreDirectorFactorySupplier) {
        if (Stream.of(easyScoreDirectorFactorySupplier, constraintStreamScoreDirectorFactorySupplier,
                incrementalScoreDirectorFactorySupplier)
                .filter(Objects::nonNull).count() > 1) {
            List<String> scoreDirectorFactoryPropertyList = new ArrayList<>(4);
            if (easyScoreDirectorFactorySupplier != null) {
                scoreDirectorFactoryPropertyList
                        .add("an easyScoreCalculatorClass (" + config.getEasyScoreCalculatorClass().getName() + ")");
            }
            if (constraintStreamScoreDirectorFactorySupplier != null) {
                scoreDirectorFactoryPropertyList
                        .add("a constraintProviderClass (" + config.getConstraintProviderClass().getName() + ")");
            }
            if (incrementalScoreDirectorFactorySupplier != null) {
                scoreDirectorFactoryPropertyList.add(
                        "an incrementalScoreCalculatorClass (" + config.getIncrementalScoreCalculatorClass().getName() + ")");
            }
            throw new IllegalArgumentException("The scoreDirectorFactory cannot have "
                    + String.join(" and ", scoreDirectorFactoryPropertyList) + " together.");
        }
    }

}
