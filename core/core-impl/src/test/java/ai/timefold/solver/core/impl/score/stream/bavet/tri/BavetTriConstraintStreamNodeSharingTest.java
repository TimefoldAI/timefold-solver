package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.tri.AbstractTriConstraintStreamNodeSharingTest;

final class BavetTriConstraintStreamNodeSharingTest extends AbstractTriConstraintStreamNodeSharingTest {

    public BavetTriConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
