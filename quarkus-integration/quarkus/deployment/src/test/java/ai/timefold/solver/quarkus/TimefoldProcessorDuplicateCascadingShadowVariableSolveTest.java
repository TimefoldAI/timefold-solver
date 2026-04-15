package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdomain.cascade.TestdataQuarkusDuplicateCascadingConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.cascade.TestdataQuarkusDuplicateCascadingEntity;
import ai.timefold.solver.quarkus.testdomain.cascade.TestdataQuarkusDuplicateCascadingSolution;
import ai.timefold.solver.quarkus.testdomain.cascade.TestdataQuarkusDuplicateCascadingValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorDuplicateCascadingShadowVariableSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "-3")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusDuplicateCascadingEntity.class,
                            TestdataQuarkusDuplicateCascadingValue.class,
                            TestdataQuarkusDuplicateCascadingSolution.class,
                            TestdataQuarkusDuplicateCascadingConstraintProvider.class))
            .addBuildChainCustomizer(buildChainBuilder -> {
                // Needed for unit test to check for duplicate GeneratedClassBuildItem
                buildChainBuilder.addFinal(ArtifactResultBuildItem.class);
            });

    @Inject
    SolverManager<TestdataQuarkusDuplicateCascadingSolution> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        var problem = new TestdataQuarkusDuplicateCascadingSolution();
        problem.setValueList(IntStream.range(1, 5)
                .mapToObj(i -> new TestdataQuarkusDuplicateCascadingValue("v%d".formatted(i)))
                .toList());
        problem.setEntityList(IntStream.range(1, 3)
                .mapToObj(i -> new TestdataQuarkusDuplicateCascadingEntity())
                .toList());
        var solverJob = solverManager.solve(1L, problem);
        var solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertEquals(-3, solution.getScore().score());
    }

}
