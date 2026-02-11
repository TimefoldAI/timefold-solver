package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierDuplicateConstraintProvider;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.test.api.testdomain.TestdataConstraintVerifierSecondEntity;

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
