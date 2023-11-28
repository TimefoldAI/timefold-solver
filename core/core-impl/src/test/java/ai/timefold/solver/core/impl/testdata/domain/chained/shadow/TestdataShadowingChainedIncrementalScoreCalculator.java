package ai.timefold.solver.core.impl.testdata.domain.chained.shadow;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;

public class TestdataShadowingChainedIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataShadowingChainedSolution, SimpleScore> {

    private TestdataShadowingChainedSolution workingSolution;

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
    public void resetWorkingSolution(TestdataShadowingChainedSolution workingSolution, boolean constraintMatchEnabled) {
        this.workingSolution = workingSolution;
    }

    @Override
    public void resetWorkingSolution(TestdataShadowingChainedSolution workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    private DefaultConstraintMatchTotal<SimpleScore> update(TestdataShadowingChainedSolution workingSolution) {
        var constraintRef = ConstraintRef.of(getClass().getPackageName(), "testConstraint");
        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(constraintRef, SimpleScore.ONE);
        for (var anchor : workingSolution.getChainedAnchorList()) {
            var value = countChainLength(anchor);
            constraintMatchTotal.addConstraintMatch(Collections.singletonList(anchor), SimpleScore.of(-value));
        }
        return constraintMatchTotal;
    }

    private int countChainLength(TestdataShadowingChainedObject object) {
        if (object.getNextEntity() == null) {
            return 1;
        } else { // Penalize increasing lengths increasingly more.
            return (int) Math.pow(1 + countChainLength(object.getNextEntity()), 2);
        }
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
