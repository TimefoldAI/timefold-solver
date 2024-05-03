package ai.timefold.solver.core.impl.testdata.domain.pinned.allows_unassigned;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

public class TestdataAllowsUnassignedPinningFilter
        implements PinningFilter<TestdataPinnedAllowsUnassignedSolution, TestdataPinnedAllowsUnassignedEntity> {

    @Override
    public boolean accept(TestdataPinnedAllowsUnassignedSolution solution, TestdataPinnedAllowsUnassignedEntity entity) {
        return entity.isLocked();
    }

}
