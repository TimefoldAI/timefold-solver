package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;

final class Group<OutTuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier<OutTuple_> {

    public static <OutTuple_ extends AbstractTuple, ResultContainer_> Group<OutTuple_, ResultContainer_>
            createWithoutAccumulate(Object groupKey, OutTuple_ outTuple) {
        return new Group<>(new GroupDataWithKey<>(groupKey), outTuple);
    }

    public static <OutTuple_ extends AbstractTuple, ResultContainer_> Group<OutTuple_, ResultContainer_>
            createWithoutGroupKey(ResultContainer_ resultContainer, OutTuple_ outTuple) {
        return new Group<>(new GroupDataWithAccumulate<>(resultContainer), outTuple);
    }

    public static <OutTuple_ extends AbstractTuple, ResultContainer_> Group<OutTuple_, ResultContainer_> create(Object groupKey,
            ResultContainer_ resultContainer, OutTuple_ outTuple) {
        return new Group<>(new GroupDataWithKeyAndAccumulate<>(groupKey, resultContainer), outTuple);
    }

    private final GroupData<ResultContainer_> groupData;
    private final OutTuple_ outTuple;
    public int parentCount = 1;

    private Group(GroupData<ResultContainer_> groupData, OutTuple_ outTuple) {
        this.groupData = groupData;
        this.outTuple = outTuple;
    }

    public Object getGroupKey() {
        return groupData.groupKey();
    }

    public ResultContainer_ getResultContainer() {
        return groupData.resultContainer();
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

    /**
     * Save memory by allowing to not store the group key or the result container.
     *
     * @param <ResultContainer_>
     */
    private sealed interface GroupData<ResultContainer_> {

        Object groupKey();

        ResultContainer_ resultContainer();

    }

    private record GroupDataWithKey<ResultContainer_>(Object groupKey) implements GroupData<ResultContainer_> {

        @Override
        public ResultContainer_ resultContainer() {
            throw new UnsupportedOperationException("Impossible state: no result container for group (" + groupKey + ").");
        }

    }

    private record GroupDataWithAccumulate<ResultContainer_>(
            ResultContainer_ resultContainer) implements GroupData<ResultContainer_> {

        @Override
        public Object groupKey() {
            throw new UnsupportedOperationException("Impossible state: no group key.");
        }

    }

    private record GroupDataWithKeyAndAccumulate<ResultContainer_>(Object groupKey,
            ResultContainer_ resultContainer) implements GroupData<ResultContainer_> {

    }

}
