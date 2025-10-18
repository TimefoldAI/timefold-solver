package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.uni.AbstractUniConstraintStreamPrecomputeTest;

final class BavetUniConstraintStreamPrecomputeTest extends AbstractUniConstraintStreamPrecomputeTest {

    public BavetUniConstraintStreamPrecomputeTest(ConstraintMatchPolicy constraintMatchPolicy) {
        super(new BavetConstraintStreamImplSupport(constraintMatchPolicy));
    }

}
