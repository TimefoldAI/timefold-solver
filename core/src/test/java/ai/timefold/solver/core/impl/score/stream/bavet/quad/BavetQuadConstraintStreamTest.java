package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.quad.AbstractQuadConstraintStreamTest;

final class BavetQuadConstraintStreamTest extends AbstractQuadConstraintStreamTest {

    public BavetQuadConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
