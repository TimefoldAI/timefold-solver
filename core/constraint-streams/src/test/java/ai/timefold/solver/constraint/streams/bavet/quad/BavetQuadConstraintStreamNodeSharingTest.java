package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.quad.AbstractQuadConstraintStreamNodeSharingTest;

final class BavetQuadConstraintStreamNodeSharingTest extends AbstractQuadConstraintStreamNodeSharingTest {

    public BavetQuadConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
