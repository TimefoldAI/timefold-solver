package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;

public abstract class AbstractTwoInputNode<LeftTuple_ extends AbstractTuple, RightTuple_ extends AbstractTuple>
        extends AbstractNode
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<RightTuple_> {

}
