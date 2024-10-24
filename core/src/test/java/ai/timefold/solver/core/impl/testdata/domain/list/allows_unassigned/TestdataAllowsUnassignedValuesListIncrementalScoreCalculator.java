package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

import org.jspecify.annotations.NonNull;

public final class TestdataAllowsUnassignedValuesListIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataAllowsUnassignedValuesListSolution, SimpleScore> {

    private List<TestdataAllowsUnassignedValuesListEntity> entityList;

    @Override
    public void resetWorkingSolution(@NonNull TestdataAllowsUnassignedValuesListSolution workingSolution) {
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {
        entityList.add((TestdataAllowsUnassignedValuesListEntity) entity);
    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {
        entityList.remove((TestdataAllowsUnassignedValuesListEntity) entity);
    }

    @Override
    public @NonNull SimpleScore calculateScore() {
        int i = 0;
        for (TestdataAllowsUnassignedValuesListEntity entity : entityList) {
            i += entity.getValueList().size();
        }
        return SimpleScore.of(-i);
    }
}
