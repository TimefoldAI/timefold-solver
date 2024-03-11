package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.quad.AbstractQuadConstraintStreamNodeSharingTest;

final class BavetQuadConstraintStreamNodeSharingTest extends AbstractQuadConstraintStreamNodeSharingTest {

    public BavetQuadConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
