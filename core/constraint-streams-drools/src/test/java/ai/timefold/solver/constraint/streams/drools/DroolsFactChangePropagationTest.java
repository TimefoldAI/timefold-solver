package ai.timefold.solver.constraint.streams.drools;

import ai.timefold.solver.constraint.streams.common.AbstractFactChangePropagationTest;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;

final class DroolsFactChangePropagationTest extends AbstractFactChangePropagationTest {

    public DroolsFactChangePropagationTest() {
        super(ConstraintStreamImplType.DROOLS);
    }
}
