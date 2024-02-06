package ai.timefold.solver.core.impl.testdata.domain.list.pinned.index;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

public final class TestdataPinnedWithIndexListIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataPinnedWithIndexListSolution, SimpleScore> {

    private List<TestdataPinnedWithIndexListEntity> entityList;

    @Override
    public void resetWorkingSolution(TestdataPinnedWithIndexListSolution workingSolution) {
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(Object entity) {
        entityList.add((TestdataPinnedWithIndexListEntity) entity);
    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
        // No need to do anything.
    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        // No need to do anything.
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        entityList.remove((TestdataPinnedWithIndexListEntity) entity);
    }

    @Override
    public SimpleScore calculateScore() {
        return SimpleScore.of(-entityList.size());
    }
}
