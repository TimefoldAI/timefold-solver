package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

sealed abstract class AbstractGroup<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier
        permits GroupWithAccumulateAndGroupKey, GroupWithAccumulateWithoutGroupKey, GroupWithoutAccumulate {

    public final OutTuple_ outTuple;
    public int parentCount = 1;

    public AbstractGroup(OutTuple_ outTuple) {
        this.outTuple = outTuple;
    }

    public abstract Object getGroupKey();

    public abstract ResultContainer_ getResultContainer();

    public final OutTuple_ getOutTuple() {
        return outTuple;
    }

}
