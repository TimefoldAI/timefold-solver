package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

sealed abstract class AbstractGroup<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier<OutTuple_>
        permits GroupWithAccumulateAndGroupKey, GroupWithAccumulateWithoutGroupKey, GroupWithoutAccumulate {

    public final OutTuple_ outTuple;
    public int parentCount = 1;

    public AbstractGroup(OutTuple_ outTuple) {
        this.outTuple = outTuple;
    }

    public abstract Object getGroupKey();

    public abstract ResultContainer_ getResultContainer();

    @Override
    public OutTuple_ getTuple() {
        return outTuple;
    }

    @Override
    public TupleState getState() {
        return outTuple.state;
    }

    @Override
    public void setState(TupleState state) {
        outTuple.state = state;
    }

}
