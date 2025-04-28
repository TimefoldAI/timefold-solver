package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.pinned;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

import org.jspecify.annotations.NonNull;

public class TestdataExtendedPinningFilter
        implements PinningFilter<TestdataExtendedPinnedSolution, TestdataExtendedPinnedEntity> {

    @Override
    public boolean accept(@NonNull TestdataExtendedPinnedSolution solution, @NonNull TestdataExtendedPinnedEntity entity) {
        return entity.isClosed();
    }

}
