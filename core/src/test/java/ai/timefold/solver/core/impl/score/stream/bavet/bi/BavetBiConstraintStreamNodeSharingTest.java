package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.bi.AbstractBiConstraintStreamNodeSharingTest;

final class BavetBiConstraintStreamNodeSharingTest extends AbstractBiConstraintStreamNodeSharingTest {

    public BavetBiConstraintStreamNodeSharingTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
