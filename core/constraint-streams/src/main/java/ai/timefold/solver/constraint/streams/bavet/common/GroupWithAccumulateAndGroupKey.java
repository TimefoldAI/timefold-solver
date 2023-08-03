package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

final class GroupWithAccumulateAndGroupKey<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractGroup<OutTuple_, ResultContainer_> {

    private final ResultContainer_ resultContainer;
    private final Object groupKey;

    public GroupWithAccumulateAndGroupKey(Object groupKey, ResultContainer_ resultContainer, OutTuple_ outTuple) {
        super(outTuple);
        this.resultContainer = resultContainer;
        this.groupKey = groupKey;
    }

    @Override
    public ResultContainer_ getResultContainer() {
        return resultContainer;
    }

    @Override
    public Object getGroupKey() {
        return groupKey;
    }

    @Override
    public String toString() {
        return "GroupWithAccumulateAndGroupKey{" +
                "groupKey=" + groupKey +
                ", outTuple=" + outTuple +
                "} " + super.toString();
    }
}
