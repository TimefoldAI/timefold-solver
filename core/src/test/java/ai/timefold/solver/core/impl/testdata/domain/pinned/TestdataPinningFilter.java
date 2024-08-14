package ai.timefold.solver.core.impl.testdata.domain.pinned;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

import org.jspecify.annotations.NonNull;

public class TestdataPinningFilter implements PinningFilter<TestdataPinnedSolution, TestdataPinnedEntity> {

    @Override
    public boolean accept(TestdataPinnedSolution solution, @NonNull TestdataPinnedEntity entity) {
        return entity.isLocked();
    }

}
