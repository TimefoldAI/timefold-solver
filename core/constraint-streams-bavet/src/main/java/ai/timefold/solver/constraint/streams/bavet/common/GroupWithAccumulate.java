package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

final class GroupWithAccumulate<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractGroup<OutTuple_, ResultContainer_> {

    private final ResultContainer_ resultContainer;

    public GroupWithAccumulate(Object groupKey, ResultContainer_ resultContainer, OutTuple_ outTuple) {
        super(groupKey, outTuple);
        this.resultContainer = resultContainer;
    }

    @Override
    public ResultContainer_ getResultContainer() {
        return resultContainer;
    }

}
