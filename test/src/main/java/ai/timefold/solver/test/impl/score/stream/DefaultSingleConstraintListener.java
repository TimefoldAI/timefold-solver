package ai.timefold.solver.test.impl.score.stream;

import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.test.api.score.stream.SingleConstraintAssertion;
import ai.timefold.solver.test.api.score.stream.SingleConstraintListener;

public final class DefaultSingleConstraintListener<Solution_, Score_ extends Score<Score_>>
        extends DefaultSingleConstraintAssertion<Solution_, Score_> implements SingleConstraintListener {

    private final AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;
    private final Solution_ solution;

    DefaultSingleConstraintListener(AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            Solution_ solution,
            Score_ score, Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        super(scoreDirectorFactory, score, constraintMatchTotalMap, indictmentMap);
        this.scoreDirectorFactory = scoreDirectorFactory;
        this.solution = Objects.requireNonNull(solution);
    }

    @Override
    public SingleConstraintAssertion settingAllShadowVariables() {
        try (var scoreDirector = scoreDirectorFactory.buildDerivedScoreDirector(true, ConstraintMatchPolicy.ENABLED)) {
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
            update(scoreDirector.calculateScore(), scoreDirector.getConstraintMatchTotalMap(), scoreDirector.getIndictmentMap());
            return this;
        }
    }
}
