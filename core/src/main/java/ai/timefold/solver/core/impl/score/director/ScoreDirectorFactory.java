package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector.AbstractScoreDirectorBuilder;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Builds {@link AbstractScoreDirector} instances, which in turn calculate the {@link Score} of a solution.
 * <p>
 * Building the factory is potentially expensive,
 * as it eagerly does whatever the score calculation implementation needs to be set up,
 * such as building the entire constraint network of a {@link ConstraintProvider}.
 * Building a score director out of an existing factory is comparatively cheap.
 * Therefore, a factory is built once and shared,
 * while every consumer that needs to calculate a score gets its own score director.
 * <p>
 * Every factory is bound to a default {@link EnvironmentMode},
 * the one it was built for.
 * {@link #createScoreDirectorBuilder()} builds for that mode,
 * whereas {@link #createScoreDirectorBuilder(EnvironmentMode)} builds for the requested mode instead;
 * the latter exists because a solver phase may override the solver's environment mode.
 * Implementations must guarantee that the returned builder produces a score director
 * that actually runs in the requested mode,
 * even if that means the implementation cannot reuse the state it built for its default mode.
 * {@link MultiEnvironmentScoreDirectorFactory} is the implementation which handles that situation,
 * and therefore the one solver components are expected to hold on to
 * whenever the config has a phase running in a mode of its own.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public interface ScoreDirectorFactory<Solution_, Score_ extends Score<Score_>> {

    SolutionDescriptor<Solution_> getSolutionDescriptor();

    ScoreDefinition<Score_> getScoreDefinition();

    /**
     * Prepares a score director which runs in the given environment mode,
     * regardless of the mode this factory was built for.
     *
     * @param environmentMode the environment mode the resulting score director must run in
     */
    AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder(EnvironmentMode environmentMode);

    /**
     * As defined by {@link #createScoreDirectorBuilder(EnvironmentMode)},
     * using the environment mode this factory was built for.
     */
    AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder();

    /**
     * Builds a score director for the given environment mode,
     * with all builder options left at their defaults.
     * Use {@link #createScoreDirectorBuilder(EnvironmentMode)} to customize them.
     */
    default AbstractScoreDirector<Solution_, Score_, ?> buildScoreDirector(EnvironmentMode environmentMode) {
        return createScoreDirectorBuilder(environmentMode).build();
    }

    /**
     * As defined by {@link #buildScoreDirector(EnvironmentMode)},
     * using the environment mode this factory was built for.
     */
    default AbstractScoreDirector<Solution_, Score_, ?> buildScoreDirector() {
        return createScoreDirectorBuilder().build();
    }

    /**
     * @return null if the factory was not built from a {@link ScoreDirectorFactoryConfig},
     *         as is often the case in tests; solver-built factories always have a trend
     */
    @Nullable
    InitializingScoreTrend getInitializingScoreTrend();

}
