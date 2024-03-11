package ai.timefold.solver.core.impl.testdata.domain.list.pinned;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

public final class TestdataPinnedListIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataPinnedListSolution, SimpleScore> {

    private List<TestdataPinnedListEntity> entityList;

    @Override
    public void resetWorkingSolution(TestdataPinnedListSolution workingSolution) {
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(Object entity) {
        entityList.add((TestdataPinnedListEntity) entity);
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
        entityList.remove((TestdataPinnedListEntity) entity);
    }

    @Override
    public SimpleScore calculateScore() {
        return SimpleScore.of(-entityList.size());
    }
}
