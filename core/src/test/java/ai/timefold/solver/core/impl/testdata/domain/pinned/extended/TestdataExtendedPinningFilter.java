package ai.timefold.solver.core.impl.testdata.domain.pinned.extended;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

import org.jspecify.annotations.NonNull;

public class TestdataExtendedPinningFilter
        implements PinningFilter<TestdataExtendedPinnedSolution, TestdataExtendedPinnedEntity> {

    @Override
    public boolean accept(@NonNull TestdataExtendedPinnedSolution solution, @NonNull TestdataExtendedPinnedEntity entity) {
        return entity.isClosed();
    }

}
