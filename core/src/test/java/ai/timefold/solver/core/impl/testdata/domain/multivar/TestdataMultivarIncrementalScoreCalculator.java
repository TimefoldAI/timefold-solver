package ai.timefold.solver.core.impl.testdata.domain.multivar;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;

public class TestdataMultivarIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataMultiVarSolution, SimpleScore> {

    private TestdataMultiVarSolution workingSolution;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(TestdataMultiVarSolution workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    @Override
    public void resetWorkingSolution(TestdataMultiVarSolution workingSolution, boolean constraintMatchEnabled) {
        this.workingSolution = workingSolution;
        this.indictmentMap = null;
    }

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
        return update().getScore();
    }

    private DefaultConstraintMatchTotal<SimpleScore> update() {
        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(
                ConstraintRef.of(getClass().getPackageName(), "testConstraint"),
                SimpleScore.ONE);
        this.indictmentMap = new HashMap<>();
        for (TestdataMultiVarEntity left : workingSolution.getMultiVarEntityList()) {
            int count = left.getPrimaryValue() == left.getSecondaryValue() ? 0 : 1;
            count += left.getTertiaryValueAllowedUnassigned() == null ? 0 : 1;
            var constraintMatch = constraintMatchTotal.addConstraintMatch(List.of(left), SimpleScore.of(-count));
            indictmentMap.computeIfAbsent(left, key -> new DefaultIndictment<>(key, SimpleScore.ZERO))
                    .getConstraintMatchSet()
                    .add(constraintMatch);
        }
        return constraintMatchTotal;
    }

    @Override
    public Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        return Collections.singleton(update());
    }

    @Override
    public Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        update();
        return indictmentMap;
    }
}
