package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThatCode;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.quarkus.testdata.multiscore.domain.TestdataMultipleScoreSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMultipleScoreProviderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackages(true, "ai.timefold.solver.quarkus.testdata.multiscore"));

    @Inject
    SolverFactory<TestdataMultipleScoreSolution> solverFactory;

    @Test
    void readOnlyConcreteProviderClass() {
        var problem = TestdataMultipleScoreSolution.generateSolution(3, 2);
        assertThatCode(() -> solverFactory.buildSolver().solve(problem)).doesNotThrowAnyException();
    }
}
