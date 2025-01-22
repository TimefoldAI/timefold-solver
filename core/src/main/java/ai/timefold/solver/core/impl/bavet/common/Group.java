package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

final class Group<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier<OutTuple_> {

    public static <OutTuple_ extends AbstractTuple, ResultContainer_> Group<OutTuple_, ResultContainer_>
            createWithoutAccumulate(Object groupKey, OutTuple_ outTuple) {
        return new Group<>(groupKey, null, outTuple);
    }

    public static <OutTuple_ extends AbstractTuple, ResultContainer_> Group<OutTuple_, ResultContainer_>
            createWithoutGroupKey(ResultContainer_ resultContainer, OutTuple_ outTuple) {
        return new Group<>(null, resultContainer, outTuple);
    }

    public static <OutTuple_ extends AbstractTuple, ResultContainer_> Group<OutTuple_, ResultContainer_> create(Object groupKey,
            ResultContainer_ resultContainer, OutTuple_ outTuple) {
        return new Group<>(groupKey, resultContainer, outTuple);
    }

    private final Object groupKey;
    private final ResultContainer_ resultContainer;
    private final OutTuple_ outTuple;
    public int parentCount = 1;

    private Group(Object groupKey, ResultContainer_ resultContainer, OutTuple_ outTuple) {
        this.groupKey = groupKey;
        this.resultContainer = resultContainer;
        this.outTuple = outTuple;
    }

    public Object getGroupKey() {
        return groupKey;
    }

    public ResultContainer_ getResultContainer() {
        return resultContainer;
    }

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
