package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.testdata.chained.constraints.TestdataChainedQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.chained.domain.TestdataChainedQuarkusAnchor;
import ai.timefold.solver.quarkus.testdata.chained.domain.TestdataChainedQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.chained.domain.TestdataChainedQuarkusObject;
import ai.timefold.solver.quarkus.testdata.chained.domain.TestdataChainedQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class OptaPlannerProcessorChainedXMLNoneTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            TestdataChainedQuarkusObject.class,
                            TestdataChainedQuarkusAnchor.class,
                            TestdataChainedQuarkusEntity.class,
                            TestdataChainedQuarkusSolution.class,
                            TestdataChainedQuarkusConstraintProvider.class));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataChainedQuarkusSolution> solverFactory;

    @Test
    void solverConfigXml_default() {
        assertThat(solverConfig).isNotNull();
        assertThat(solverConfig.getSolutionClass()).isEqualTo(TestdataChainedQuarkusSolution.class);
        assertThat(solverConfig.getEntityClassList()).containsExactlyInAnyOrder(
                TestdataChainedQuarkusObject.class,
                TestdataChainedQuarkusEntity.class);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                .isEqualTo(TestdataChainedQuarkusConstraintProvider.class);
        // No termination defined (solverConfig.xml isn't included)
        assertThat(solverConfig.getTerminationConfig().getSecondsSpentLimit()).isNull();
        assertThat(solverFactory).isNotNull();
        assertThat(solverFactory.buildSolver()).isNotNull();
    }

}
