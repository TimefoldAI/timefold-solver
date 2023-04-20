package ai.timefold.solver.test.impl.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.DefaultScoreExplanation;
import ai.timefold.solver.test.api.score.stream.MultiConstraintAssertion;

public final class DefaultMultiConstraintAssertion<Score_ extends Score<Score_>>
        implements MultiConstraintAssertion {

    private final ConstraintProvider constraintProvider;
    private final Score_ actualScore;
    private final Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection;
    private final Collection<Indictment<Score_>> indictmentCollection;

    DefaultMultiConstraintAssertion(ConstraintProvider constraintProvider, Score_ actualScore,
            Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        this.constraintProvider = requireNonNull(constraintProvider);
        this.actualScore = requireNonNull(actualScore);
        this.constraintMatchTotalCollection = requireNonNull(constraintMatchTotalMap).values();
        this.indictmentCollection = requireNonNull(indictmentMap).values();
    }

    @Override
    public void scores(Score<?> score, String message) {
        if (actualScore.equals(score)) {
            return;
        }
        Class<?> constraintProviderClass = constraintProvider.getClass();
        String expectation = message == null ? "Broken expectation." : message;
        throw new AssertionError(expectation + System.lineSeparator() +
                "  Constraint provider: " + constraintProviderClass + System.lineSeparator() +
                "       Expected score: " + score + " (" + score.getClass() + ")" + System.lineSeparator() +
                "         Actual score: " + actualScore + " (" + actualScore.getClass() + ")" +
                System.lineSeparator() + System.lineSeparator() +
                "  " + DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection,
                        indictmentCollection));
    }

}
