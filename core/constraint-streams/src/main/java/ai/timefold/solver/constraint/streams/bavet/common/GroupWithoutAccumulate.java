package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

final class GroupWithoutAccumulate<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractGroup<OutTuple_, ResultContainer_> {

    private final Object groupKey;

    public GroupWithoutAccumulate(Object groupKey, OutTuple_ outTuple) {
        super(outTuple);
        this.groupKey = groupKey;
    }

    @Override
    public ResultContainer_ getResultContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getGroupKey() {
        return groupKey;
    }

    @Override
    public String toString() {
        return "GroupWithoutAccumulate{" +
                "groupKey=" + groupKey +
                ", outTuple=" + outTuple +
                "} " + super.toString();
    }
}
