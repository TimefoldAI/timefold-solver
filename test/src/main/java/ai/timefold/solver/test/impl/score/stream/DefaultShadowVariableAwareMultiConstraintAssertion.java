package ai.timefold.solver.test.impl.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.MultiConstraintAssertion;
import ai.timefold.solver.test.api.score.stream.ShadowVariableAwareMultiConstraintAssertion;

public final class DefaultShadowVariableAwareMultiConstraintAssertion<Solution_, Score_ extends Score<Score_>>
        extends AbstractMultiConstraintAssertion<Solution_, Score_> implements ShadowVariableAwareMultiConstraintAssertion {

    private final AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory;
    private final Solution_ solution;

    DefaultShadowVariableAwareMultiConstraintAssertion(ConstraintProvider constraintProvider,
            AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory,
            Solution_ solution) {
        super(constraintProvider, scoreDirectorFactory);
        this.scoreDirectorFactory = requireNonNull(scoreDirectorFactory);
        this.solution = Objects.requireNonNull(solution);
    }

    @Override
    public MultiConstraintAssertion settingAllShadowVariables() {
        // Most score directors don't need derived status; CS will override this.
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withConstraintMatchPolicy(ConstraintMatchPolicy.ENABLED)
                .buildDerived()) {
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
            update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(),
                    scoreDirector.getIndictmentMap());
            toggleInitialized();
            return this;
        }
    }

    @Override
    Solution_ getSolution() {
        return solution;
    }
}
