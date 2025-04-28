package ai.timefold.solver.core.testdomain.shadow;

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
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestdataShadowedIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataShadowedSolution, SimpleScore> {

    private TestdataShadowedSolution workingSolution;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(@NonNull TestdataShadowedSolution workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    @Override
    public void resetWorkingSolution(@NonNull TestdataShadowedSolution workingSolution, boolean constraintMatchEnabled) {
        this.workingSolution = workingSolution;
        this.indictmentMap = null;
    }

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
        return update().getScore();
    }

    private DefaultConstraintMatchTotal<SimpleScore> update() {
        var constraintMatchTotal = new DefaultConstraintMatchTotal<>(
                ConstraintRef.of(getClass().getPackageName(), "testConstraint"),
                SimpleScore.ONE);
        this.indictmentMap = new HashMap<>();
        for (TestdataShadowedEntity left : workingSolution.getEntityList()) {
            TestdataValue value = left.getValue();
            if (value == null) {
                continue;
            }
            for (TestdataShadowedEntity right : workingSolution.getEntityList()) {
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
    public @NonNull Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        return Collections.singleton(update());
    }

    @Override
    public @Nullable Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        update();
        return indictmentMap;
    }
}
