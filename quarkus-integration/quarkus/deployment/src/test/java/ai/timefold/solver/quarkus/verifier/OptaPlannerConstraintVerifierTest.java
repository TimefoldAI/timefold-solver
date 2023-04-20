package ai.timefold.solver.quarkus.verifier;

import java.util.Arrays;

import javax.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class OptaPlannerConstraintVerifierTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class));

    @Inject
    ConstraintVerifier<TestdataQuarkusConstraintProvider, TestdataQuarkusSolution> constraintVerifier;

    @Test
    void constraintVerifierDroolsStreamImpl() {
        TestdataQuarkusSolution solution = new TestdataQuarkusSolution();
        TestdataQuarkusEntity entityA = new TestdataQuarkusEntity();
        TestdataQuarkusEntity entityB = new TestdataQuarkusEntity();
        entityA.setValue("A");
        entityB.setValue("A");

        solution.setEntityList(Arrays.asList(
                entityA, entityB));
        solution.setValueList(Arrays.asList("A", "B"));
        constraintVerifier.verifyThat().givenSolution(solution).scores(SimpleScore.of(-2));

        entityB.setValue("B");
        constraintVerifier.verifyThat().givenSolution(solution).scores(SimpleScore.ZERO);
    }
}
