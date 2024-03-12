package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.tri.AbstractTriConstraintStreamTest;

final class BavetTriConstraintStreamTest extends AbstractTriConstraintStreamTest {

    public BavetTriConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new BavetConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
