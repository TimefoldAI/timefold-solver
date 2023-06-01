package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.tri.AbstractTriConstraintStreamTest;

final class BavetTriConstraintStreamTest extends AbstractTriConstraintStreamTest {

    public BavetTriConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
