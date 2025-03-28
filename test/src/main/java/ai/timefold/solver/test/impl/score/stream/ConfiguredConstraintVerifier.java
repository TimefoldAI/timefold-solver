package ai.timefold.solver.test.impl.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.UUID;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

final class ConfiguredConstraintVerifier<ConstraintProvider_ extends ConstraintProvider, Solution_, Score_ extends Score<Score_>> {

    /**
     * Exists so that people can not, even by accident, pick the same constraint ID as the default cache key.
     */
    private final ConstraintRef defaultScoreDirectorFactoryMapKey =
            ConstraintRef.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    private final ConstraintProvider_ constraintProvider;
    private final ScoreDirectorFactoryCache<ConstraintProvider_, Solution_, Score_> scoreDirectorFactoryContainer;

    public ConfiguredConstraintVerifier(ConstraintProvider_ constraintProvider,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        this.constraintProvider = constraintProvider;
        this.scoreDirectorFactoryContainer = new ScoreDirectorFactoryCache<>(solutionDescriptor);
    }

    public DefaultSingleConstraintVerification<Solution_, Score_> verifyThat(
            BiFunction<ConstraintProvider_, ConstraintFactory, Constraint> constraintFunction) {
        requireNonNull(constraintFunction);
        var scoreDirectorFactory = scoreDirectorFactoryContainer.getScoreDirectorFactory(constraintFunction, constraintProvider,
                EnvironmentMode.FULL_ASSERT);
        return new DefaultSingleConstraintVerification<>(scoreDirectorFactory);
    }

    public DefaultMultiConstraintVerification<Solution_, Score_> verifyThat() {
        var scoreDirectorFactory = scoreDirectorFactoryContainer.getScoreDirectorFactory(defaultScoreDirectorFactoryMapKey,
                constraintProvider, EnvironmentMode.FULL_ASSERT);
        return new DefaultMultiConstraintVerification<>(scoreDirectorFactory, constraintProvider);
    }

}
