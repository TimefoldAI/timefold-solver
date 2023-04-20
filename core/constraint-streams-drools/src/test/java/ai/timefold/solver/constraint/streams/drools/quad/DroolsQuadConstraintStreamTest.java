package ai.timefold.solver.constraint.streams.drools.quad;

import ai.timefold.solver.constraint.streams.common.quad.AbstractQuadConstraintStreamTest;
import ai.timefold.solver.constraint.streams.drools.DroolsConstraintStreamImplSupport;

final class DroolsQuadConstraintStreamTest extends AbstractQuadConstraintStreamTest {

    public DroolsQuadConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new DroolsConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
