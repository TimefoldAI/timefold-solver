package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.uni.AbstractUniConstraintStreamNodeSharingTest;

final class BavetUniConstraintStreamNodeSharingTest extends AbstractUniConstraintStreamNodeSharingTest {

    public BavetUniConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
