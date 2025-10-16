package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.tri.AbstractTriConstraintStreamPrecomputeTest;

final class BavetTriConstraintStreamPrecomputeTest extends AbstractTriConstraintStreamPrecomputeTest {

    public BavetTriConstraintStreamPrecomputeTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
