package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

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
    private interface GroupData<ResultContainer_> {

        Object groupKey();

        ResultContainer_ resultContainer();

    }

    private static final class GroupDataWithKey<ResultContainer_> implements GroupData<ResultContainer_> {

        private final Object groupKey;

        public GroupDataWithKey(Object groupKey) {
            this.groupKey = groupKey;
        }

        public Object groupKey() {
            return groupKey;
        }

        @Override
        public ResultContainer_ resultContainer() {
            throw new UnsupportedOperationException("Impossible state: no result container for group (" + groupKey + ").");
        }

    }

    private static final class GroupDataWithAccumulate<ResultContainer_> implements GroupData<ResultContainer_> {

        private final ResultContainer_ resultContainer;

        public GroupDataWithAccumulate(ResultContainer_ resultContainer) {
            this.resultContainer = resultContainer;
        }

        @Override
        public Object groupKey() {
            throw new UnsupportedOperationException("Impossible state: no group key.");
        }

        @Override
        public ResultContainer_ resultContainer() {
            return resultContainer;
        }

    }

    private static final class GroupDataWithKeyAndAccumulate<ResultContainer_> implements GroupData<ResultContainer_> {

        private final Object groupKey;
        private final ResultContainer_ resultContainer;

        public GroupDataWithKeyAndAccumulate(Object groupKey, ResultContainer_ resultContainer) {
            this.groupKey = groupKey;
            this.resultContainer = resultContainer;
        }

        @Override
        public Object groupKey() {
            return groupKey;
        }

        @Override
        public ResultContainer_ resultContainer() {
            return resultContainer;
        }
    }

}
