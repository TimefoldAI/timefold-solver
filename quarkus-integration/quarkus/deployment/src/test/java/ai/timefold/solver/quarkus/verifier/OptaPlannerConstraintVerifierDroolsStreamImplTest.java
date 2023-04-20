package ai.timefold.solver.quarkus.verifier;

import java.util.Arrays;

import javax.inject.Inject;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import ai.timefold.solver.test.impl.score.stream.DefaultConstraintVerifier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class OptaPlannerConstraintVerifierDroolsStreamImplTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class,
                            TestdataQuarkusSolution.class, TestdataQuarkusConstraintProvider.class)
                    .addAsResource("org/optaplanner/quarkus/verifier/droolsSolverConfig.xml", "solverConfig.xml"));

    @Inject
    ConstraintVerifier<TestdataQuarkusConstraintProvider, TestdataQuarkusSolution> constraintVerifier;

    @Test
    void constraintVerifierDroolsStreamImpl() {
        Assertions.assertEquals(ConstraintStreamImplType.DROOLS,
                ((DefaultConstraintVerifier<?, ?, ?>) constraintVerifier)
                        .getConstraintStreamImplType());
        Assertions.assertEquals(!ConfigUtils.isNativeImage(),
                ((DefaultConstraintVerifier<?, ?, ?>) constraintVerifier)
                        .isDroolsAlphaNetworkCompilationEnabled());
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
