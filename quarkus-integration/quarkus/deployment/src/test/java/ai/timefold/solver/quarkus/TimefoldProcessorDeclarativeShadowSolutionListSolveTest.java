package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdomain.declarative.list.TestdataQuarkusDeclarativeShadowVariableListConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.declarative.list.TestdataQuarkusDeclarativeShadowVariableListEntity;
import ai.timefold.solver.quarkus.testdomain.declarative.list.TestdataQuarkusDeclarativeShadowVariableListSolution;
import ai.timefold.solver.quarkus.testdomain.declarative.list.TestdataQuarkusDeclarativeShadowVariableListValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorDeclarativeShadowSolutionListSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "-1")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusDeclarativeShadowVariableListSolution.class,
                            TestdataQuarkusDeclarativeShadowVariableListEntity.class,
                            TestdataQuarkusDeclarativeShadowVariableListValue.class,
                            TestdataQuarkusDeclarativeShadowVariableListConstraintProvider.class));
    @Inject
    SolverManager<TestdataQuarkusDeclarativeShadowVariableListSolution, Long> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        var problem = new TestdataQuarkusDeclarativeShadowVariableListSolution();
        problem.setEntityList(List.of(new TestdataQuarkusDeclarativeShadowVariableListEntity("a"),
                new TestdataQuarkusDeclarativeShadowVariableListEntity("b")));
        var a = new TestdataQuarkusDeclarativeShadowVariableListValue("1");
        var b = new TestdataQuarkusDeclarativeShadowVariableListValue("2");
        var c = new TestdataQuarkusDeclarativeShadowVariableListValue("3", List.of(a, b));
        problem.setValueList(List.of(a, b, c));
        var solverJob = solverManager.solve(1L, problem);
        var solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertNotSame(solution, problem);
        assertEquals(-1, solution.getScore().score());
        assertNotSame(solution.getEntityList().get(0), problem.getEntityList().get(1));
    }

}
