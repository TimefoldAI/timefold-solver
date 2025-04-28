package ai.timefold.solver.core.testdomain.chained.shadow;

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

public class TestdataShadowingChainedIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataShadowingChainedSolution, SimpleScore> {

    private TestdataShadowingChainedSolution workingSolution;

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
    public void resetWorkingSolution(@NonNull TestdataShadowingChainedSolution workingSolution,
            boolean constraintMatchEnabled) {
        this.workingSolution = workingSolution;
    }

    @Override
    public void resetWorkingSolution(@NonNull TestdataShadowingChainedSolution workingSolution) {
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
    public @NonNull Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        var constraintMatchTotal = update(workingSolution);
        return Collections.singleton(constraintMatchTotal);
    }

    @Override
    public @Nullable Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        throw new UnsupportedOperationException();
    }

}
