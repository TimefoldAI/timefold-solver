package ai.timefold.solver.core.impl.score.stream.test;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.constraintweightoverrides.TestdataConstraintWeightOverridesConstraintProvider;
import ai.timefold.solver.core.testdomain.constraintweightoverrides.TestdataConstraintWeightOverridesSolution;

import org.junit.jupiter.api.Test;

class NullOverridesTest {

    private final ConstraintVerifier<TestdataConstraintWeightOverridesConstraintProvider, TestdataConstraintWeightOverridesSolution> constraintVerifier =
            ConstraintVerifier.build(new TestdataConstraintWeightOverridesConstraintProvider(),
                    TestdataConstraintWeightOverridesSolution.class,
                    TestdataEntity.class);

    @Test
    void doesNotThrowNPEOnNoOverrides() {
        constraintVerifier.verifyThat(TestdataConstraintWeightOverridesConstraintProvider::firstConstraint)
                .given()
                .rewardsWith(0);
    }

}
