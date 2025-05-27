package ai.timefold.solver.core.impl.solver;

import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class MoveAssertScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, MoveAssertScoreDirectorFactory<Solution_, Score_>> {

    protected MoveAssertScoreDirector(MoveAssertScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            boolean lookUpEnabled,
            ConstraintMatchPolicy constraintMatchPolicy,
            boolean expectShadowVariablesInCorrectState) {
        super(scoreDirectorFactory, lookUpEnabled, constraintMatchPolicy, expectShadowVariablesInCorrectState);
    }

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        super.setWorkingSolution(workingSolution, ignored -> {
        });
    }

    @Override
    public InnerScore<Score_> calculateScore() {
        return InnerScore.fullyAssigned(scoreDirectorFactory.getScoreDefinition().getZeroScore());
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return Map.of();
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return Map.of();
    }

    @Override
    public boolean requiresFlushing() {
        return false;
    }
}
