package ai.timefold.solver.core.impl.testdata.domain.pinned;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

public class TestdataPinningFilter implements PinningFilter<TestdataPinnedSolution, TestdataPinnedEntity> {

    @Override
    public boolean accept(TestdataPinnedSolution solution, TestdataPinnedEntity entity) {
        return entity.isLocked();
    }

}
