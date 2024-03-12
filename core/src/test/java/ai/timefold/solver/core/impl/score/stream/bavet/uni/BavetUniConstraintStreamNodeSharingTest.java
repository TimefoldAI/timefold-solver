package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.uni.AbstractUniConstraintStreamNodeSharingTest;

final class BavetUniConstraintStreamNodeSharingTest extends AbstractUniConstraintStreamNodeSharingTest {

    public BavetUniConstraintStreamNodeSharingTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
