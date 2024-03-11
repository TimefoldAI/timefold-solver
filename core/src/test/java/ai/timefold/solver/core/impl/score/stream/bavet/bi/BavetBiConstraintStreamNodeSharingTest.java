package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.bi.AbstractBiConstraintStreamNodeSharingTest;

final class BavetBiConstraintStreamNodeSharingTest extends AbstractBiConstraintStreamNodeSharingTest {

    public BavetBiConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
