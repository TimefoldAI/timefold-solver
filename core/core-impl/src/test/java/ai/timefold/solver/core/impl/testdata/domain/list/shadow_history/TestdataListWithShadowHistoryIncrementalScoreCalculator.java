package ai.timefold.solver.core.impl.testdata.domain.list.shadow_history;

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

public class TestdataListWithShadowHistoryIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataListSolutionWithShadowHistory, SimpleScore> {

    private int score = 0;
    private DefaultConstraintMatchTotal<SimpleScore> constraintMatchTotal;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(TestdataListSolutionWithShadowHistory workingSolution) {
        score = 0;
        constraintMatchTotal = new DefaultConstraintMatchTotal<>(
                ConstraintRef.of("ai.timefold.solver.core.impl.testdata.domain.chained.shadow", "testConstraint"));
        indictmentMap = new HashMap<>();
        for (TestdataListEntityWithShadowHistory left : workingSolution.getEntityList()) {
            String code = left.getCode();
            if (code == null) {
                continue;
            }
            for (TestdataListEntityWithShadowHistory right : workingSolution.getEntityList()) {
                if (Objects.equals(right.getCode(), code)) {
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
    public void resetWorkingSolution(TestdataListSolutionWithShadowHistory workingSolution, boolean constraintMatchEnabled) {
        resetWorkingSolution(workingSolution);
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
        return SimpleScore.of(score);
    }

    @Override
    public Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        return Collections.singleton(constraintMatchTotal);
    }

    @Override
    public Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        return indictmentMap;
    }
}
