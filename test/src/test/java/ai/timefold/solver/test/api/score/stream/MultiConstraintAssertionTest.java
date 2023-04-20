package ai.timefold.solver.test.api.score.stream;

import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierConstraintProvider;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.test.api.score.stream.testdata.TestdataConstraintVerifierSecondEntity;

import org.junit.jupiter.api.Test;

class MultiConstraintAssertionTest {

    private final ConstraintVerifier<TestdataConstraintVerifierConstraintProvider, TestdataConstraintVerifierExtendedSolution> constraintVerifier =
            ConstraintVerifier.build(new TestdataConstraintVerifierConstraintProvider(),
                    TestdataConstraintVerifierExtendedSolution.class,
                    TestdataConstraintVerifierFirstEntity.class,
                    TestdataConstraintVerifierSecondEntity.class);

    @Test
    void checksScore() {
        TestdataConstraintVerifierExtendedSolution solution = TestdataConstraintVerifierExtendedSolution.generateSolution(4, 5);

        assertThatCode(() -> constraintVerifier.verifyThat()
                .givenSolution(solution)
                .scores(HardSoftScore.of(-15, 3), "There should be no penalties"))
                .doesNotThrowAnyException();
        assertThatCode(() -> constraintVerifier.verifyThat()
                .givenSolution(solution)
                .scores(HardSoftScore.of(1, 1), "There should be penalties"))
                .hasMessageContaining("There should be penalties");
    }

}
