package ai.timefold.solver.core.impl.score.director;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector.AbstractScoreDirectorBuilder;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Decorates the {@link ScoreDirectorFactory} built for the solver's environment mode,
 * so that a phase running in a different mode can be served a factory built for <i>its</i> mode.
 * <p>
 * A score director factory is built for exactly one {@link EnvironmentMode}.
 * For an easy or incremental score factories that hardly matters,
 * the mode is passed straight on to the score director,
 * but {@link BavetConstraintStreamScoreDirectorFactory} builds its constraint network from the mode in its constructor,
 * so asking it for a score director in another mode would yield one which reports that mode
 * while running the network of the mode it was built for.
 * This class removes that trap by building a separate factory per mode.
 * <p>
 * Building one is expensive,
 * so each is cached.
 * The cache holds only the modes other than the solver's global factory.
 * The cached factories are concrete ones, never further decorators.
 * <p>
 * This is a decorator, not a place to configure a factory:
 * the {@link InitializingScoreTrend} and the assertion score director factory are read through to the decorated factory,
 * and setting either here fails,
 * because {@link ScoreDirectorFactoryFactory} applies both before wrapping
 * and a value set afterwards would reach neither the decorated factory nor the cached ones.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type to go with the solution
 * @param <Factory_> the decorated factory's own type
 */
@NullMarked
public class MultiEnvironmentScoreDirectorFactory<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>>
        extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_> {

    private final ScoreDirectorFactoryFactory<Solution_, Score_> scoreDirectorFactoryFactory;
    private final AbstractScoreDirectorFactory<Solution_, Score_, Factory_> globalScoreDirectorFactory;
    /**
     * Only holds modes other than the global one;
     * the global mode's factory is {@link #globalScoreDirectorFactory}.
     */
    private final Map<EnvironmentMode, AbstractScoreDirectorFactory<Solution_, Score_, ?>> environmentModeToFactoryMap =
            new ConcurrentHashMap<>();

    public MultiEnvironmentScoreDirectorFactory(ScoreDirectorFactoryFactory<Solution_, Score_> scoreDirectorFactoryFactory,
            AbstractScoreDirectorFactory<Solution_, Score_, Factory_> globalScoreDirectorFactory,
            EnvironmentMode globalEnvironmentMode) {
        super(globalScoreDirectorFactory.getSolutionDescriptor(), globalEnvironmentMode);
        this.scoreDirectorFactoryFactory = scoreDirectorFactoryFactory;
        this.globalScoreDirectorFactory = globalScoreDirectorFactory;
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return globalScoreDirectorFactory.getSolutionDescriptor();
    }

    @Override
    public ScoreDefinition<Score_> getScoreDefinition() {
        return globalScoreDirectorFactory.getScoreDefinition();
    }

    @Override
    public @Nullable InitializingScoreTrend getInitializingScoreTrend() {
        return globalScoreDirectorFactory.getInitializingScoreTrend();
    }

    @Override
    public void setInitializingScoreTrend(InitializingScoreTrend initializingScoreTrend) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ScoreDirectorFactory<Solution_, Score_> getAssertionScoreDirectorFactory() {
        return globalScoreDirectorFactory.getAssertionScoreDirectorFactory();
    }

    @Override
    public void setAssertionScoreDirectorFactory(ScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the undecorated factory built for the solver's environment mode,
     *         for the callers outside the solving life cycle which expect a concrete implementation,
     *         such as {@code BeanUtil#buildConstraintMetaModel} needing a constraint stream factory
     */
    public ScoreDirectorFactory<Solution_, Score_> getInnerScoreDirectorFactory() {
        return globalScoreDirectorFactory;
    }

    @Override
    public AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder() {
        return createScoreDirectorBuilder(globalEnvironmentMode);
    }

    @Override
    public AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder(EnvironmentMode environmentMode) {
        if (environmentMode == globalEnvironmentMode) {
            return globalScoreDirectorFactory.createScoreDirectorBuilder(environmentMode);
        }
        // Build the concrete factory rather than going through buildScoreDirectorFactory(..),
        // which would decorate it again for a mode this instance already handles.
        var factory = environmentModeToFactoryMap.computeIfAbsent(environmentMode,
                mode -> scoreDirectorFactoryFactory.buildConcreteScoreDirectorFactory(mode, getSolutionDescriptor()));
        return factory.createScoreDirectorBuilder();
    }
}
