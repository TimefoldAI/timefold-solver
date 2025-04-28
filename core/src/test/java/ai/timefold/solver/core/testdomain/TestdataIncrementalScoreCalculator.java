package ai.timefold.solver.core.testdomain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestdataIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataSolution, SimpleScore> {

    private int score = 0;
    private DefaultConstraintMatchTotal<SimpleScore> constraintMatchTotal;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(@NonNull TestdataSolution workingSolution) {
        score = 0;
        constraintMatchTotal = new DefaultConstraintMatchTotal<>(
                ConstraintRef.of("ai.timefold.solver.core.testdomain", "testConstraint"), SimpleScore.ONE);
        indictmentMap = new HashMap<>();
        for (TestdataEntity left : workingSolution.getEntityList()) {
            TestdataValue value = left.getValue();
            if (value == null) {
                continue;
            }
            for (TestdataEntity right : workingSolution.getEntityList()) {
                if (Objects.equals(right.getValue(), value)) {
                    score -= 1;
                    ConstraintMatch<SimpleScore> constraintMatch =
                            constraintMatchTotal.addConstraintMatch(List.of(left, right), SimpleScore.ONE);
                    Stream.of(left, right)
                            .forEach(entity -> indictmentMap
                                    .computeIfAbsent(entity, key -> new DefaultIndictment<>(key, SimpleScore.ZERO))
                                    .getConstraintMatchSet()
                                    .add(constraintMatch));
                }
            }
        }
    }

    @Override
    public void resetWorkingSolution(@NonNull TestdataSolution workingSolution, boolean constraintMatchEnabled) {
        resetWorkingSolution(workingSolution);
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
        return SimpleScore.of(score);
    }

    @Override
    public @NonNull Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        return Collections.singleton(constraintMatchTotal);
    }

    @Override
    public @Nullable Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        return indictmentMap;
    }
}
