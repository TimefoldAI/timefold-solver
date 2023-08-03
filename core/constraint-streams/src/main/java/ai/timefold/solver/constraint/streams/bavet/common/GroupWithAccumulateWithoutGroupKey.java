package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

final class GroupWithAccumulateWithoutGroupKey<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractGroup<OutTuple_, ResultContainer_> {

    private final ResultContainer_ resultContainer;

    public GroupWithAccumulateWithoutGroupKey(ResultContainer_ resultContainer, OutTuple_ outTuple) {
        super(outTuple);
        this.resultContainer = resultContainer;
    }

    @Override
    public Object getGroupKey() {
        throw new UnsupportedOperationException("Impossible state: requested group key on a tuple which can have none.");
    }

    @Override
    public ResultContainer_ getResultContainer() {
        return resultContainer;
    }

    @Override
    public String toString() {
        return "GroupWithAccumulateWithoutGroupKey{" +
                "outTuple=" + outTuple +
                "} " + super.toString();
    }
}
