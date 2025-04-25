package ai.timefold.solver.core.testdomain.pinned;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

import org.jspecify.annotations.NonNull;

public class TestdataPinningFilter implements PinningFilter<TestdataPinnedSolution, TestdataPinnedEntity> {

    @Override
    public boolean accept(@NonNull TestdataPinnedSolution solution, @NonNull TestdataPinnedEntity entity) {
        return entity.isLocked();
    }

}
