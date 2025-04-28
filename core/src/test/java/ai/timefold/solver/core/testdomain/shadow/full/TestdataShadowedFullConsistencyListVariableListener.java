package ai.timefold.solver.core.testdomain.shadow.full;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestdataShadowedFullConsistencyListVariableListener
        implements ListVariableListener<TestdataShadowedFullSolution, TestdataShadowedFullEntity, TestdataShadowedFullValue> {
    @Override
    public void afterListVariableElementUnassigned(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullValue value) {
        // Ignore
    }

    @Override
    public void beforeListVariableChanged(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullEntity entity, int fromIndex, int toIndex) {
        checkRange(entity, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullEntity entity, int fromIndex, int toIndex) {
        checkRange(entity, fromIndex, toIndex);
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullEntity entity) {
        // Ignore
    }

    @Override
    public void afterEntityAdded(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullEntity entity) {
        // Ignore
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullEntity entity) {
        // Ignore
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<TestdataShadowedFullSolution> scoreDirector,
            TestdataShadowedFullEntity entity) {
        // Ignore
    }

    private void checkRange(TestdataShadowedFullEntity entity, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > entity.valueList.size()) {
            throw new IllegalStateException("Change range [%s, %s) went past bounds of entity list %s"
                    .formatted(fromIndex, toIndex, entity.valueList));
        }
    }
}
