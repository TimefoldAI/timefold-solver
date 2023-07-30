package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

sealed abstract class AbstractGroup<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier
        permits GroupWithAccumulate, GroupWithoutAccumulate {

    public final Object groupKey;
    public final OutTuple_ outTuple;
    public int parentCount = 1;

    public AbstractGroup(Object groupKey, OutTuple_ outTuple) {
        this.groupKey = groupKey;
        this.outTuple = outTuple;
    }

    public final Object getGroupKey() {
        return groupKey;
    }

    public abstract ResultContainer_ getResultContainer();

    public final OutTuple_ getOutTuple() {
        return outTuple;
    }

    @Override
    public final String toString() {
        return Objects.toString(groupKey);
    }

}
