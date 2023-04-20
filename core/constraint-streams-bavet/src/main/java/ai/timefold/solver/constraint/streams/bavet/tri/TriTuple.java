package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.Tuple;

public interface TriTuple<A, B, C> extends Tuple {

    A getFactA();

    B getFactB();

    C getFactC();

}
