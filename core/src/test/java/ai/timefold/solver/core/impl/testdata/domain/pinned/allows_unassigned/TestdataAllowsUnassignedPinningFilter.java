package ai.timefold.solver.core.impl.testdata.domain.pinned.allows_unassigned;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

import org.jspecify.annotations.NonNull;

public class TestdataAllowsUnassignedPinningFilter
        implements PinningFilter<TestdataPinnedAllowsUnassignedSolution, TestdataPinnedAllowsUnassignedEntity> {

    @Override
    public boolean accept(@NonNull TestdataPinnedAllowsUnassignedSolution solution,
            @NonNull TestdataPinnedAllowsUnassignedEntity entity) {
        return entity.isLocked();
    }

}
