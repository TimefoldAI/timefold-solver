package ai.timefold.solver.test.impl.score.stream;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.MultiConstraintVerification;

public final class DefaultMultiConstraintVerification<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintVerification<Solution_, Score_>
        implements MultiConstraintVerification<Solution_> {

    private final ConstraintProvider constraintProvider;

    DefaultMultiConstraintVerification(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            ConstraintProvider constraintProvider) {
        super(scoreDirectorFactory);
        this.constraintProvider = constraintProvider;
    }

    @Override
    public DefaultMultiConstraintAssertion<Score_> given(Object... facts) {
        assertCorrectArguments(facts);
        return sessionBasedAssertionBuilder.multiConstraintGiven(constraintProvider, facts);
    }

    @Override
    public DefaultMultiConstraintAssertion<Score_> givenSolution(Solution_ solution) {
        try (var scoreDirector = scoreDirectorFactory.buildDerivedScoreDirector(true, true)) {
            scoreDirector.setWorkingSolution(Objects.requireNonNull(solution));
            return new DefaultMultiConstraintAssertion<>(constraintProvider, scoreDirector.calculateScore(),
                    scoreDirector.getConstraintMatchTotalMap(), scoreDirector.getIndictmentMap());
        }
    }

}
