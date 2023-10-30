package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.bi.AbstractBiConstraintStreamNodeSharingTest;

final class BavetBiConstraintStreamNodeSharingTest extends AbstractBiConstraintStreamNodeSharingTest {

    public BavetBiConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
