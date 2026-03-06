package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdomain.spec.TestdataSpecConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.spec.TestdataSpecEntity;
import ai.timefold.solver.quarkus.testdomain.spec.TestdataSpecProducer;
import ai.timefold.solver.quarkus.testdomain.spec.TestdataSpecSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorPlanningSpecificationSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataSpecEntity.class, TestdataSpecSolution.class,
                            TestdataSpecConstraintProvider.class, TestdataSpecProducer.class));

    @Inject
    SolverFactory<TestdataSpecSolution> solverFactory;
    @Inject
    SolverManager<TestdataSpecSolution> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        assertNotNull(solverFactory);
        assertNotNull(solverManager);

        TestdataSpecSolution problem = new TestdataSpecSolution();
        problem.setValueList(IntStream.range(1, 3)
                .mapToObj(i -> "v" + i)
                .collect(Collectors.toList()));
        problem.setEntityList(IntStream.range(1, 3)
                .mapToObj(i -> new TestdataSpecEntity())
                .collect(Collectors.toList()));
        SolverJob<TestdataSpecSolution> solverJob = solverManager.solve(1L, problem);
        TestdataSpecSolution solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertTrue(solution.getScore().score() >= 0);
    }

}
