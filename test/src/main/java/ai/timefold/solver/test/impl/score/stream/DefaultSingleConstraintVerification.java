package ai.timefold.solver.test.impl.score.stream;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.SingleConstraintVerification;

import org.jspecify.annotations.NonNull;

public final class DefaultSingleConstraintVerification<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintVerification<Solution_, Score_>
        implements SingleConstraintVerification<Solution_> {

    DefaultSingleConstraintVerification(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory) {
        super(scoreDirectorFactory);
    }

    @Override
    public @NonNull DefaultSingleConstraintAssertion<Solution_, Score_> given(@NonNull Object @NonNull... facts) {
        assertCorrectArguments(facts);
        return sessionBasedAssertionBuilder.singleConstraintGiven(facts);
    }

    @Override
    public @NonNull DefaultSingleConstraintAssertion<Solution_, Score_> givenSolution(@NonNull Solution_ solution) {
        try (var scoreDirector = scoreDirectorFactory.buildDerivedScoreDirector(true, ConstraintMatchPolicy.ENABLED)) {
            scoreDirector.setWorkingSolution(Objects.requireNonNull(solution));
            return new DefaultSingleConstraintAssertion<>(scoreDirectorFactory, scoreDirector.calculateScore(),
                    scoreDirector.getConstraintMatchTotalMap(), scoreDirector.getIndictmentMap());
        }
    }

}
