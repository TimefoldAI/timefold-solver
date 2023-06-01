package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;

final class GroupWithoutAccumulate<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractGroup<OutTuple_, ResultContainer_> {

    public GroupWithoutAccumulate(Object groupKey, OutTuple_ outTuple) {
        super(groupKey, outTuple);
    }

    @Override
    public ResultContainer_ getResultContainer() {
        throw new UnsupportedOperationException();
    }

}
