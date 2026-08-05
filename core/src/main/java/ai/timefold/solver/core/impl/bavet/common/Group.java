package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleList;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

final class Group<InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier<OutTuple_> {

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> createWithoutAccumulate(Object groupKey, OutTuple_ outTuple,
                    TupleList<InTuple_> contributors) {
        return new Group<>(groupKey, null, outTuple, contributors);
    }

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> createWithoutGroupKey(ResultContainer_ resultContainer,
                    OutTuple_ outTuple, TupleList<InTuple_> contributors) {
        return new Group<>(null, resultContainer, outTuple, contributors);
    }

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> create(Object groupKey, ResultContainer_ resultContainer,
                    OutTuple_ outTuple, TupleList<InTuple_> contributors) {
        return new Group<>(groupKey, resultContainer, outTuple, contributors);
    }

    private final Object groupKey;
    private final ResultContainer_ resultContainer;
    private final OutTuple_ outTuple;
    /**
     * The input tuples currently mapped into this group. Backs membership tracking only ({@link #isEmpty()}
     * is a group's sole liveness answer now that groupBy no longer needs to answer "is this group
     * transitively still live" for a downstream join/ifExists) -- a plain count would do the same job
     * more cheaply, since nothing here needs to walk individual contributors any more.
     */
    private final TupleList<InTuple_> contributors;

    private Group(Object groupKey, ResultContainer_ resultContainer, OutTuple_ outTuple, TupleList<InTuple_> contributors) {
        this.groupKey = groupKey;
        this.resultContainer = resultContainer;
        this.outTuple = outTuple;
        this.contributors = contributors;
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
        contributors.add(tuple);
    }

    public void removeContributor(InTuple_ tuple) {
        contributors.remove(tuple);
    }

    public boolean isEmpty() {
        return contributors.size() == 0;
    }

}
