package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.tri.AbstractTriConstraintStreamNodeSharingTest;

final class BavetTriConstraintStreamNodeSharingTest extends AbstractTriConstraintStreamNodeSharingTest {

    public BavetTriConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
