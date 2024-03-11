package ai.timefold.solver.core.impl.testdata.domain.list.shadow_history;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;

public class TestdataListWithShadowHistoryIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataListSolutionWithShadowHistory, SimpleScore> {

    private TestdataListSolutionWithShadowHistory workingSolution;

    @Override
    public void beforeEntityAdded(Object entity) {

    }

    @Override
    public void afterEntityAdded(Object entity) {

    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {

    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {

    }

    @Override
    public void beforeEntityRemoved(Object entity) {

    }

    @Override
    public void afterEntityRemoved(Object entity) {

    }

    @Override
    public SimpleScore calculateScore() {
        var constraintMatchTotal = update(workingSolution);
        return constraintMatchTotal.getScore();
    }

    @Override
    public void resetWorkingSolution(TestdataListSolutionWithShadowHistory workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    @Override
    public void resetWorkingSolution(TestdataListSolutionWithShadowHistory workingSolution, boolean constraintMatchEnabled) {
        this.workingSolution = workingSolution;
    }

    private DefaultConstraintMatchTotal<SimpleScore> update(TestdataListSolutionWithShadowHistory workingSolution) {
        var constraintRef = ConstraintRef.of(getClass().getPackageName(), "testConstraint");
        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(constraintRef, SimpleScore.ONE);
        for (var e : workingSolution.getEntityList()) {
            int value = (int) Math.pow(e.getValueList().size(), 2);
            if (value != 0) {
                constraintMatchTotal.addConstraintMatch(Collections.singletonList(e), SimpleScore.of(-value));
            }
        }
        return constraintMatchTotal;
    }

    @Override
    public Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        var constraintMatchTotal = update(workingSolution);
        return Collections.singleton(constraintMatchTotal);
    }

    @Override
    public Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        throw new UnsupportedOperationException();
    }
}
