package ai.timefold.solver.test.impl.score.stream;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.test.api.score.stream.MultiConstraintAssertion;
import ai.timefold.solver.test.api.score.stream.MultipleConstraintListener;

public final class DefaultMultiConstraintListener<Solution_, Score_ extends Score<Score_>>
        extends DefaultMultiConstraintAssertion<Score_> implements MultipleConstraintListener {

    private final InnerScoreDirector<Solution_, Score_> scoreDirector;

    DefaultMultiConstraintListener(ConstraintProvider constraintProvider,
            InnerScoreDirector<Solution_, Score_> scoreDirector, Score_ actualScore,
            Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap,
            Map<Object, Indictment<Score_>> indictmentMap) {
        super(constraintProvider, actualScore, constraintMatchTotalMap, indictmentMap);
        this.scoreDirector = requireNonNull(scoreDirector);
    }

    @Override
    public MultiConstraintAssertion settingAllShadowVariables() {
        scoreDirector.triggerVariableListeners();
        return this;
    }
}
