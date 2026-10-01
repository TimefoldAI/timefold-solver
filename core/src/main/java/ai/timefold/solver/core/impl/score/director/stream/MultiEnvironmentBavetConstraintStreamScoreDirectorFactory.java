package ai.timefold.solver.core.impl.score.director.stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;

import org.jspecify.annotations.NullMarked;

/**
 * The factory the solver holds whenever a phase overrides its environment mode.
 * It takes over from the factory built for the solver's own mode (global environment mode),
 * sharing its constraint network, and builds a separate factory for every other mode asked of it.
 * Building one is expensive, so each is cached;
 * the cache holds only the other modes, since the global mode is served by this instance itself.
 * The cached factories are the concrete ones the config implies, never further adaptations.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public final class MultiEnvironmentBavetConstraintStreamScoreDirectorFactory<Solution_, Score_ extends Score<Score_>> extends
        BavetConstraintStreamScoreDirectorFactory<Solution_, Score_> {

    private final ScoreDirectorFactoryFactory<Solution_, Score_> scoreDirectorFactoryFactory;
    /**
     * Only holds modes other than the global one, which this instance serves itself.
     */
    private final Map<EnvironmentMode, AbstractScoreDirectorFactory<Solution_, Score_, ?>> environmentModeToFactoryMap =
            new ConcurrentHashMap<>();

    public MultiEnvironmentBavetConstraintStreamScoreDirectorFactory(
            ScoreDirectorFactoryFactory<Solution_, Score_> scoreDirectorFactoryFactory,
            BavetConstraintStreamScoreDirectorFactory<Solution_, Score_> globalScoreDirectorFactory) {
        super(globalScoreDirectorFactory);
        this.scoreDirectorFactoryFactory = scoreDirectorFactoryFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BavetConstraintStreamScoreDirector.Builder<Solution_, Score_>
            createScoreDirectorBuilder(EnvironmentMode environmentMode) {
        if (environmentMode == globalEnvironmentMode) {
            // The inherited constraint network was built for this mode.
            return super.createScoreDirectorBuilder(environmentMode);
        }
        // Build the concrete factory rather than going through buildScoreDirectorFactory(..),
        // which would adapt it again for a mode this instance already serves.
        var factory = environmentModeToFactoryMap.computeIfAbsent(environmentMode,
                mode -> scoreDirectorFactoryFactory.buildConcreteScoreDirectorFactory(mode, getSolutionDescriptor()));
        // The factory was built for this mode, so its own builder is the one which runs in it.
        return (BavetConstraintStreamScoreDirector.Builder<Solution_, Score_>) factory.createScoreDirectorBuilder();
    }

}
