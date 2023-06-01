package ai.timefold.solver.constraint.streams.bavet;

import ai.timefold.solver.constraint.streams.common.AbstractFactChangePropagationTest;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;

final class BavetFactChangePropagationTest extends AbstractFactChangePropagationTest {

    public BavetFactChangePropagationTest() {
        super(ConstraintStreamImplType.BAVET);
    }
}
