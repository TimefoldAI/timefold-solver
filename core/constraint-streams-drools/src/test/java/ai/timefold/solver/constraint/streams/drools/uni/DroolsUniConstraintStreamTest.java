package ai.timefold.solver.constraint.streams.drools.uni;

import ai.timefold.solver.constraint.streams.common.uni.AbstractUniConstraintStreamTest;
import ai.timefold.solver.constraint.streams.drools.DroolsConstraintStreamImplSupport;

final class DroolsUniConstraintStreamTest extends AbstractUniConstraintStreamTest {

    public DroolsUniConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new DroolsConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
