package ai.timefold.solver.core.impl.testdata.domain.pinned.extended;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

public class TestdataExtendedPinningFilter
        implements PinningFilter<TestdataExtendedPinnedSolution, TestdataExtendedPinnedEntity> {

    @Override
    public boolean accept(TestdataExtendedPinnedSolution solution, TestdataExtendedPinnedEntity entity) {
        return entity.isClosed();
    }

}
