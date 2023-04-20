package ai.timefold.solver.constraint.streams.drools.tri;

import ai.timefold.solver.constraint.streams.common.tri.AbstractTriConstraintStreamTest;
import ai.timefold.solver.constraint.streams.drools.DroolsConstraintStreamImplSupport;

final class DroolsTriConstraintStreamTest extends AbstractTriConstraintStreamTest {

    public DroolsTriConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new DroolsConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
