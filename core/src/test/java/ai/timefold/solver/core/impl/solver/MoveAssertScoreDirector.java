package ai.timefold.solver.core.impl.solver;

import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class MoveAssertScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, MoveAssertScoreDirectorFactory<Solution_, Score_>> {
    private final Consumer<Solution_> moveSolutionConsumer;
    private boolean firstTrigger = true;
    private final boolean isDerived;

    protected MoveAssertScoreDirector(MoveAssertScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            boolean lookUpEnabled,
            ConstraintMatchPolicy constraintMatchPolicy,
            boolean expectShadowVariablesInCorrectState,
            Consumer<Solution_> moveSolutionConsumer,
            boolean isDerived) {
        super(scoreDirectorFactory, lookUpEnabled, constraintMatchPolicy, expectShadowVariablesInCorrectState);
        this.moveSolutionConsumer = moveSolutionConsumer;
        this.isDerived = isDerived;
    }

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        super.setWorkingSolution(workingSolution, ignored -> {
        });
    }

    @Override
    public boolean isDerived() {
        return isDerived;
    }

    @Override
    public InnerScore<Score_> calculateScore() {
        if (!isDerived && firstTrigger) {
            moveSolutionConsumer.accept(getWorkingSolution());
            firstTrigger = false;
        }
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
