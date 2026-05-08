package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public abstract class AbstractTwoInputNode<LeftTuple_ extends Tuple, RightTuple_ extends Tuple>
        extends AbstractNode
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<RightTuple_> {

    private final TupleLifecycle<?> downstreamTupleLifecycle;

    private boolean leftInitialized;
    private boolean rightInitialized;
    protected boolean fullyInitialized;
    protected boolean leftCanProduceTuples;
    protected boolean rightCanProduceTuples;

    protected AbstractTwoInputNode(TupleLifecycle<?> downstreamTupleLifecycle) {
        this.downstreamTupleLifecycle = downstreamTupleLifecycle;

    }

    @Override
    public final void initializeLeft(boolean upstreamCanProduceTuples) {
        leftCanProduceTuples = upstreamCanProduceTuples;
        if (!fullyInitialized && rightInitialized) {
            // Only initialize downstream nodes when we have received initialization from both parents.
            // Avoid initializing twice.
            downstreamTupleLifecycle.initialize(isActive());
            fullyInitialized = true;
        }
        leftInitialized = true;
    }

    @Override
    public final void initializeRight(boolean upstreamCanProduceTuples) {
        rightCanProduceTuples = upstreamCanProduceTuples;
        if (!fullyInitialized && leftInitialized) {
            // Only initialize downstream nodes when we have received initialization from both parents.
            // Avoid initializing twice.
            downstreamTupleLifecycle.initialize(isActive());
            fullyInitialized = true;
        }
        rightInitialized = true;
    }

}
