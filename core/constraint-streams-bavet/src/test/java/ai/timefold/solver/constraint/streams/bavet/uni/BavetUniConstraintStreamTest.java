package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.uni.AbstractUniConstraintStreamTest;

final class BavetUniConstraintStreamTest extends AbstractUniConstraintStreamTest {

    public BavetUniConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
