package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.bi.AbstractBiConstraintStreamPrecomputeTest;

final class BavetBiConstraintStreamPrecomputeTest extends AbstractBiConstraintStreamPrecomputeTest {

    public BavetBiConstraintStreamPrecomputeTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
