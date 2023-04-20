
package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.common.Tuple;

public interface QuadTuple<A, B, C, D> extends Tuple {

    A getFactA();

    B getFactB();

    C getFactC();

    D getFactD();

}
