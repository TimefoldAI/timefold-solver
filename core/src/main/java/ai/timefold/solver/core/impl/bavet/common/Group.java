package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

final class Group<InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier<OutTuple_> {

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> createWithoutAccumulate(Object groupKey, OutTuple_ outTuple) {
        return new Group<>(groupKey, null, outTuple);
    }

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> createWithoutGroupKey(ResultContainer_ resultContainer,
                    OutTuple_ outTuple) {
        return new Group<>(null, resultContainer, outTuple);
    }

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> create(Object groupKey, ResultContainer_ resultContainer,
                    OutTuple_ outTuple) {
        return new Group<>(groupKey, resultContainer, outTuple);
    }

    private final Object groupKey;
    private final ResultContainer_ resultContainer;
    private final OutTuple_ outTuple;
    /**
     * How many input tuples are currently mapped into this group. The only thing this backs is
     * {@link #isEmpty()} -- a group's sole liveness answer -- so a plain counter is enough: nothing ever
     * needs to walk or even retain the individual contributors.
     */
    private int contributorCount;

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
        return outTuple.getState();
    }

    @Override
    public void setState(TupleState state) {
        outTuple.setState(state);
    }

    public void addContributor(InTuple_ tuple) {
        contributorCount++;
    }

    public void removeContributor(InTuple_ tuple) {
        contributorCount--;
    }

    public boolean isEmpty() {
        return contributorCount == 0;
    }

}
