package ai.timefold.solver.core.impl.score.stream.common.quad;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.AbstractConstraintBuilderTest;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintBuilder;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.testdomain.TestdataSolution;

class QuadConstraintBuilderTest extends AbstractConstraintBuilderTest {

    private static final BavetConstraintFactory<TestdataSolution> CONSTRAINT_FACTORY =
            new BavetConstraintFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.FULL_ASSERT);

    @Override
    protected AbstractConstraintBuilder<SimpleScore> of(String constraintName, String constraintGroup) {
        return new QuadConstraintBuilderImpl<>(
                (constraintName1, constraintDescription, constraintGroup1, constraintWeight, impactType,
                        objectSimpleScoreObjectBiFunction,
                        objectCollectionFunction) -> new BavetConstraint<>(CONSTRAINT_FACTORY,
                                ConstraintRef.of(constraintName1), constraintDescription, constraintGroup1, constraintWeight,
                                impactType, objectSimpleScoreObjectBiFunction, objectCollectionFunction, null),
                ScoreImpactType.PENALTY, SimpleScore.ONE);
    }
}
