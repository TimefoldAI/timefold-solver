package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

public final class TestdataAllowsUnassignedValuesListIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataAllowsUnassignedValuesListSolution, SimpleScore> {

    private List<TestdataAllowsUnassignedValuesListEntity> entityList;

    @Override
    public void resetWorkingSolution(TestdataAllowsUnassignedValuesListSolution workingSolution) {
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(Object entity) {
        entityList.add((TestdataAllowsUnassignedValuesListEntity) entity);
    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        throw new UnsupportedOperationException(); // Will not be called.
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        entityList.remove((TestdataAllowsUnassignedValuesListEntity) entity);
    }

    @Override
    public SimpleScore calculateScore() {
        int i = 0;
        for (TestdataAllowsUnassignedValuesListEntity entity : entityList) {
            i += entity.getValueList().size();
        }
        return SimpleScore.of(-i);
    }
}
