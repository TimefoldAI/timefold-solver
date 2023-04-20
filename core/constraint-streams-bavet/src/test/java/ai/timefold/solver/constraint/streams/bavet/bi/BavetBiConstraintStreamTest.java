package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.constraint.streams.common.bi.AbstractBiConstraintStreamTest;

final class BavetBiConstraintStreamTest extends AbstractBiConstraintStreamTest {

    public BavetBiConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
