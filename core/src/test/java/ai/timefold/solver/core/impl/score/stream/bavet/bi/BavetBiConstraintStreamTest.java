package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.bi.AbstractBiConstraintStreamTest;

final class BavetBiConstraintStreamTest extends AbstractBiConstraintStreamTest {

    public BavetBiConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
