package ai.timefold.solver.core.impl.testdata.domain.list.pinned.index;

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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestdataPinnedWithIndexListCMAIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataPinnedWithIndexListSolution, SimpleScore> {

    private TestdataPinnedWithIndexListSolution workingSolution;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(@NonNull TestdataPinnedWithIndexListSolution workingSolution) {
        resetWorkingSolution(workingSolution, true);
    }

    @Override
    public void resetWorkingSolution(@NonNull TestdataPinnedWithIndexListSolution workingSolution,
            boolean constraintMatchEnabled) {
        this.workingSolution = workingSolution;
        this.indictmentMap = null;
    }

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {

    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {

    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {

    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {

    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {

    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {

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
        for (TestdataPinnedWithIndexListValue left : workingSolution.getValueList()) {
            TestdataPinnedWithIndexListEntity entity = left.getEntity();
            if (entity == null) {
                continue;
            }
            for (TestdataPinnedWithIndexListValue right : workingSolution.getValueList()) {
                if (Objects.equals(right.getEntity(), entity)) {
                    var constraintMatch =
                            constraintMatchTotal.addConstraintMatch(List.of(left, right), SimpleScore.ONE.negate());
                    Stream.of(left, right)
                            .forEach(value -> indictmentMap
                                    .computeIfAbsent(value, key -> new DefaultIndictment<>(key, SimpleScore.ZERO))
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
