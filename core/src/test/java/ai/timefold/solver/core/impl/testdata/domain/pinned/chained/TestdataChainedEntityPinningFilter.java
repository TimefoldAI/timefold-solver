package ai.timefold.solver.core.impl.testdata.domain.pinned.chained;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

import org.jspecify.annotations.NonNull;

public class TestdataChainedEntityPinningFilter
        implements PinningFilter<TestdataPinnedChainedSolution, TestdataPinnedChainedEntity> {

    @Override
    public boolean accept(@NonNull TestdataPinnedChainedSolution scoreDirector, @NonNull TestdataPinnedChainedEntity entity) {
        return entity.isPinned();
    }

}
