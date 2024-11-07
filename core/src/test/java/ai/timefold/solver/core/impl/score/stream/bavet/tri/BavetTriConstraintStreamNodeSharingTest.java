package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.tri.AbstractTriConstraintStreamNodeSharingTest;

final class BavetTriConstraintStreamNodeSharingTest extends AbstractTriConstraintStreamNodeSharingTest {

    public BavetTriConstraintStreamNodeSharingTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
