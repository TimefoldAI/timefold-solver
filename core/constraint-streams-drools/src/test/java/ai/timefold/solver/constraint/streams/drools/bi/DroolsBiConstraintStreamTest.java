package ai.timefold.solver.constraint.streams.drools.bi;

import ai.timefold.solver.constraint.streams.common.bi.AbstractBiConstraintStreamTest;
import ai.timefold.solver.constraint.streams.drools.DroolsConstraintStreamImplSupport;

final class DroolsBiConstraintStreamTest extends AbstractBiConstraintStreamTest {

    public DroolsBiConstraintStreamTest(boolean constraintMatchEnabled) {
        super(new DroolsConstraintStreamImplSupport(constraintMatchEnabled));
    }

}
