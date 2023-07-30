package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;

final class GroupPropagationQueue<Tuple_ extends AbstractTuple, ResultContainer_>
        extends AbstractDynamicPropagationQueue<AbstractGroup<Tuple_, ResultContainer_>, Tuple_> {

    private final Consumer<AbstractGroup<Tuple_, ResultContainer_>> groupProcessor;

    public GroupPropagationQueue(TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
            Consumer<AbstractGroup<Tuple_, ResultContainer_>> groupProcessor) {
        super(nextNodesTupleLifecycle);
        this.groupProcessor = groupProcessor;
    }

    @Override
    protected Tuple_ extractTuple(AbstractGroup<Tuple_, ResultContainer_> group) {
        return group.outTuple;
    }

    @Override
    protected TupleState extractState(AbstractGroup<Tuple_, ResultContainer_> group) {
        return group.outTuple.state;
    }

    @Override
    public void changeState(AbstractGroup<Tuple_, ResultContainer_> group, TupleState state) {
        group.outTuple.state = state;
    }

    @Override
    protected void propagateInsertOrUpdate(AbstractGroup<Tuple_, ResultContainer_> group, Consumer<Tuple_> propagator) {
        if (groupProcessor != null) {
            groupProcessor.accept(group);
        }
        super.propagateInsertOrUpdate(group, propagator);
    }

}
