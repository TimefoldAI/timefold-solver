package ai.timefold.solver.core.testdomain.list.shadowhistory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestdataListWithShadowHistoryIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataListSolutionWithShadowHistory, SimpleScore> {

    private TestdataListSolutionWithShadowHistory workingSolution;

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        // Ignore
    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        // Ignore
    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {
        // Ignore
    }

    @Override
    public @NonNull SimpleScore calculateScore() {
        var constraintMatchTotal = update(workingSolution);
        return constraintMatchTotal.getScore();
    }

    @Override
    public void resetWorkingSolution(@NonNull TestdataListSolutionWithShadowHistory workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    @Override
    public void resetWorkingSolution(@NonNull TestdataListSolutionWithShadowHistory workingSolution,
            boolean constraintMatchEnabled) {
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
    public @NonNull Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        var constraintMatchTotal = update(workingSolution);
        return Collections.singleton(constraintMatchTotal);
    }

    @Override
    public @Nullable Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        throw new UnsupportedOperationException();
    }
}
