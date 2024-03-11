package ai.timefold.solver.core.impl.score.stream.bavet;

import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.impl.score.stream.common.AbstractFactChangePropagationTest;

final class BavetFactChangePropagationTest extends AbstractFactChangePropagationTest {

    public BavetFactChangePropagationTest() {
        super(ConstraintStreamImplType.BAVET);
    }
}
