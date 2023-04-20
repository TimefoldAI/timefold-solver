package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.quad.AbstractQuadConstraintStreamTest;

final class BavetQuadConstraintStreamTest extends AbstractQuadConstraintStreamTest {

    public BavetQuadConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
