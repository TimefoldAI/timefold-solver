package ai.timefold.solver.test.impl.score.stream;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.ShadowVariableAwareSingleConstraintAssertion;
import ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion;

public final class DefaultShadowVariableAwareSingleConstraintAssertion<Solution_, Score_ extends Score<Score_>>
        extends AbstractSingleConstraintAssertion<Solution_, Score_> implements ShadowVariableAwareSingleConstraintAssertion {

    private final AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory;
    private final Solution_ solution;

    DefaultShadowVariableAwareSingleConstraintAssertion(
            AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, ?> scoreDirectorFactory,
            Solution_ solution) {
        super(scoreDirectorFactory);
        this.scoreDirectorFactory = scoreDirectorFactory;
        this.solution = Objects.requireNonNull(solution);
    }

    @Override
    public SingleConstraintAssertion settingAllShadowVariables() {
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
