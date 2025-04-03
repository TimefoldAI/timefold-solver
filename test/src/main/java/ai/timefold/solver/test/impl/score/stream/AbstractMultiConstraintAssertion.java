package ai.timefold.solver.test.impl.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.DefaultScoreExplanation;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.MultiConstraintAssertion;

import org.jspecify.annotations.NonNull;

public abstract sealed class AbstractMultiConstraintAssertion<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintAssertion<Solution_, Score_>
        implements MultiConstraintAssertion
        permits DefaultMultiConstraintAssertion, DefaultShadowVariableAwareMultiConstraintAssertion {

    private final ConstraintProvider constraintProvider;
    private Score_ actualScore;
    private Collection<ConstraintMatchTotal<Score_>> constraintMatchTotalCollection;
    private Collection<Indictment<Score_>> indictmentCollection;

    AbstractMultiConstraintAssertion(ConstraintProvider constraintProvider,
            AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory) {
        super(scoreDirectorFactory);
        this.constraintProvider = requireNonNull(constraintProvider);
    }

    @Override
    final void update(Score_ score, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        this.actualScore = requireNonNull(score);
        this.constraintMatchTotalCollection = requireNonNull(constraintMatchTotalMap).values();
        this.indictmentCollection = requireNonNull(indictmentMap).values();
        toggleInitialized();
    }

    @Override
    public void scores(@NonNull Score<?> score, String message) {
        ensureInitialized();
        if (actualScore.equals(score)) {
            return;
        }
        Class<?> constraintProviderClass = constraintProvider.getClass();
        String expectation = message == null ? "Broken expectation." : message;
        throw new AssertionError(
                "%s%s  Constraint provider: %s%s       Expected score: %s (%s)%s         Actual score: %s (%s)%s%s  %s"
                        .formatted(expectation, System.lineSeparator(), constraintProviderClass, System.lineSeparator(), score,
                                score.getClass(), System.lineSeparator(), actualScore, actualScore.getClass(),
                                System.lineSeparator(), System.lineSeparator(),
                                DefaultScoreExplanation.explainScore(actualScore, constraintMatchTotalCollection,
                                        indictmentCollection)));
    }

}
