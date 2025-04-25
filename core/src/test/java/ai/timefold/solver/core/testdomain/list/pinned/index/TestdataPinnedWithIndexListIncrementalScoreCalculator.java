package ai.timefold.solver.core.testdomain.list.pinned.index;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;

import org.jspecify.annotations.NonNull;

public final class TestdataPinnedWithIndexListIncrementalScoreCalculator
        implements IncrementalScoreCalculator<TestdataPinnedWithIndexListSolution, SimpleScore> {

    private List<TestdataPinnedWithIndexListEntity> entityList;

    @Override
    public void resetWorkingSolution(@NonNull TestdataPinnedWithIndexListSolution workingSolution) {
        this.entityList = new ArrayList<>(workingSolution.getEntityList());
    }

    @Override
    public void beforeEntityAdded(@NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityAdded(@NonNull Object entity) {
        entityList.add((TestdataPinnedWithIndexListEntity) entity);
    }

    @Override
    public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        // No need to do anything.
    }

    @Override
    public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
        // No need to do anything.
    }

    @Override
    public void beforeEntityRemoved(@NonNull Object entity) {
        // No need to do anything.
    }

    @Override
    public void afterEntityRemoved(@NonNull Object entity) {
        entityList.remove((TestdataPinnedWithIndexListEntity) entity);
    }

    @Override
    public @NonNull SimpleScore calculateScore() {
        return SimpleScore.of(-entityList.size());
    }
}
