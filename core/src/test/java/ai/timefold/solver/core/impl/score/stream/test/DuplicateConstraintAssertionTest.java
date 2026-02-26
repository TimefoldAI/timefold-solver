package ai.timefold.solver.core.impl.score.stream.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierDuplicateConstraintProvider;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierSecondEntity;

import org.junit.jupiter.api.Test;

class DuplicateConstraintAssertionTest {

    private final ConstraintVerifier<TestdataConstraintVerifierDuplicateConstraintProvider, TestdataConstraintVerifierExtendedSolution> constraintVerifier =
            ConstraintVerifier.build(new TestdataConstraintVerifierDuplicateConstraintProvider(),
                    TestdataConstraintVerifierExtendedSolution.class,
                    TestdataConstraintVerifierFirstEntity.class,
                    TestdataConstraintVerifierSecondEntity.class);

    @Test
    void throwsExceptionOnDuplicateConstraintId() {
        assertThatThrownBy(
                () -> constraintVerifier.verifyThat(TestdataConstraintVerifierDuplicateConstraintProvider::penalizeEveryEntity))
                .hasMessageContaining("Penalize every standard entity");
    }

}
