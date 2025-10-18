package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.quad.AbstractQuadConstraintStreamPrecomputeTest;

final class BavetQuadConstraintStreamPrecomputeTest extends AbstractQuadConstraintStreamPrecomputeTest {

    public BavetQuadConstraintStreamPrecomputeTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
