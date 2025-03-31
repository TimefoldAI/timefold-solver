package ai.timefold.solver.test.impl.score.stream;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.MultiConstraintVerification;

import org.jspecify.annotations.NonNull;

public final class DefaultMultiConstraintVerification<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintVerification<Solution_, Score_>
        implements MultiConstraintVerification<Solution_> {

    private final ConstraintProvider constraintProvider;

    DefaultMultiConstraintVerification(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory,
            ConstraintProvider constraintProvider) {
        super(scoreDirectorFactory);
        this.constraintProvider = constraintProvider;
    }

    @Override
    public @NonNull DefaultMultiConstraintAssertion<Score_> given(@NonNull Object @NonNull... facts) {
        assertCorrectArguments(facts);
        return sessionBasedAssertionBuilder.multiConstraintGiven(constraintProvider, facts);
    }

    @Override
    public @NonNull DefaultMultiConstraintListener<Solution_, Score_> givenSolution(@NonNull Solution_ solution) {
        return new DefaultMultiConstraintListener<>(constraintProvider, scoreDirectorFactory, solution);
    }

}
