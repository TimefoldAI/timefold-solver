package ai.timefold.solver.core.impl.score.stream.common.bi;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.AbstractConstraintBuilderTest;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.testdomain.TestdataSolution;

class BiConstraintBuilderTest extends AbstractConstraintBuilderTest {

    private static final BavetConstraintFactory<TestdataSolution> CONSTRAINT_FACTORY =
            new BavetConstraintFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.FULL_ASSERT);

    @Override
    protected AbstractConstraintBuilder<SimpleScore> of(String constraintId) {
        return new BiConstraintBuilderImpl<>(
                (constraintMetadata, constraintWeight, impactType, justificationMapping) -> new BavetConstraint<>(
                        CONSTRAINT_FACTORY, constraintMetadata, constraintWeight, impactType, justificationMapping, null),
                ScoreImpactType.PENALTY, SimpleScore.ONE);
    }
}
