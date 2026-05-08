package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public abstract class AbstractSingleInputNode<Tuple_ extends Tuple>
        extends AbstractNode
        implements TupleLifecycle<Tuple_> {

    private final TupleLifecycle<?> downstreamTupleLifecycle;

    protected AbstractSingleInputNode(TupleLifecycle<?> downstreamTupleLifecycle) {
        this.downstreamTupleLifecycle = downstreamTupleLifecycle;
    }

    private boolean upstreamCanProduceTuples;

    @Override
    public void afterAllFactsInserted(boolean upstreamCanProduceTuples) { // We only delegate; implementations can override.
        this.upstreamCanProduceTuples = upstreamCanProduceTuples;
        downstreamTupleLifecycle.afterAllFactsInserted(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return upstreamCanProduceTuples && downstreamTupleLifecycle.isActive();
    }

}
