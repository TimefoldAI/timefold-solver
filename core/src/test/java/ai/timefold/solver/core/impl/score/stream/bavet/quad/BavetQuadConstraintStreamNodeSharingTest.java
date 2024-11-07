package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.quad.AbstractQuadConstraintStreamNodeSharingTest;

final class BavetQuadConstraintStreamNodeSharingTest extends AbstractQuadConstraintStreamNodeSharingTest {

    public BavetQuadConstraintStreamNodeSharingTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
