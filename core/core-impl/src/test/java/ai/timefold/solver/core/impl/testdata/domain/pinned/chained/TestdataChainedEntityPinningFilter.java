package ai.timefold.solver.core.impl.testdata.domain.pinned.chained;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;

public class TestdataChainedEntityPinningFilter
        implements PinningFilter<TestdataPinnedChainedSolution, TestdataPinnedChainedEntity> {

    @Override
    public boolean accept(TestdataPinnedChainedSolution scoreDirector, TestdataPinnedChainedEntity entity) {
        return entity.isPinned();
    }

}
