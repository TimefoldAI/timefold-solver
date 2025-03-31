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

import org.jspecify.annotations.NonNull;

public abstract sealed class AbstractMultiConstraintAssertion<Score_ extends Score<Score_>>
        implements MultiConstraintAssertion permits DefaultMultiConstraintAssertion, DefaultMultiConstraintListener {

    private final ConstraintProvider constraintProvider;
    private Score_ actualScore;
    private Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection;
    private Collection<Indictment<Score_>> indictmentCollection;

    AbstractMultiConstraintAssertion(ConstraintProvider constraintProvider) {
        this.constraintProvider = requireNonNull(constraintProvider);
    }

    void update(Score_ actualScore, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        this.actualScore = requireNonNull(actualScore);
        this.constraintMatchTotalCollection = requireNonNull(constraintMatchTotalMap).values();
        this.indictmentCollection = requireNonNull(indictmentMap).values();
    }

    abstract void ensureInitialized();

    @Override
    public void scores(@NonNull Score<?> score, String message) {
        ensureInitialized();
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
