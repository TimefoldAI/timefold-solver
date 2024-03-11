package ai.timefold.solver.core.impl.testdata.domain.allows_unassigned;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class TestdataAllowsUnassignedIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataAllowsUnassignedSolution, SimpleScore> {

    private TestdataAllowsUnassignedSolution workingSolution;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(TestdataAllowsUnassignedSolution workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    @Override
    public void resetWorkingSolution(TestdataAllowsUnassignedSolution workingSolution, boolean constraintMatchEnabled) {
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
        for (TestdataAllowsUnassignedEntity left : workingSolution.getEntityList()) {
            TestdataValue value = left.getValue();
            for (TestdataAllowsUnassignedEntity right : workingSolution.getEntityList()) {
                if (Objects.equals(right.getValue(), value)) {
                    var constraintMatch =
                            constraintMatchTotal.addConstraintMatch(List.of(left, right), SimpleScore.ONE.negate());
                    Stream.of(left, right)
                            .forEach(entity -> indictmentMap
                                    .computeIfAbsent(entity, key -> new DefaultIndictment<>(key, SimpleScore.ZERO))
                                    .getConstraintMatchSet()
                                    .add(constraintMatch));
                }
            }
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
