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
    public TupleState extractState() {
        return outTuple.state;
    }

    @Override
    public OutTuple_ extractTuple() {
        return outTuple;
    }

    @Override
    public void changeState(TupleState state) {
        outTuple.state = state;
    }

}
