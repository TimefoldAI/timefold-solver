package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdomain.usegettersetter.TestdataQuarkusUseGetterSetterConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.usegettersetter.TestdataQuarkusUseGetterSetterEntity;
import ai.timefold.solver.quarkus.testdomain.usegettersetter.TestdataQuarkusUseGetterSetterSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorUseGettersSettersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusUseGetterSetterEntity.class,
                            TestdataQuarkusUseGetterSetterSolution.class,
                            TestdataQuarkusUseGetterSetterConstraintProvider.class));

    @Inject
    SolverManager<TestdataQuarkusUseGetterSetterSolution> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        var problem = new TestdataQuarkusUseGetterSetterSolution();
        problem.setValueList(IntStream.range(1, 3)
                .mapToObj(i -> "v" + i)
                .collect(Collectors.toList()));
        problem.setEntityList(IntStream.range(1, 3)
                .mapToObj(i -> new TestdataQuarkusUseGetterSetterEntity())
                .collect(Collectors.toList()));
        var solverJob = solverManager.solve(1L, problem);
        var solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertTrue(solution.getScore().score() >= 0);
        for (var entity : solution.getEntityList()) {
            assertTrue(entity.getGetterCallCount() > 0);
            assertTrue(entity.getSetterCallCount() > 0);
        }
    }

}
