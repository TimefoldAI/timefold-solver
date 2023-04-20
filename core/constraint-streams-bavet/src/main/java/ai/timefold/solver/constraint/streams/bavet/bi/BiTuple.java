package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.Tuple;

public interface BiTuple<A, B> extends Tuple {

    A getFactA();

    B getFactB();

}
