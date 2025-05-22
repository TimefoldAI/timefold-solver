package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.list.TestdataQuarkusSupplierVariableListConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.list.TestdataQuarkusSupplierVariableListEntity;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.list.TestdataQuarkusSupplierVariableListSolution;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.list.TestdataQuarkusSupplierVariableListValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorSupplierShadowSolutionListSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.enabled-preview-features", "DECLARATIVE_SHADOW_VARIABLES")
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "-1")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusSupplierVariableListSolution.class,
                            TestdataQuarkusSupplierVariableListEntity.class,
                            TestdataQuarkusSupplierVariableListValue.class,
                            TestdataQuarkusSupplierVariableListConstraintProvider.class));
    @Inject
    SolverManager<TestdataQuarkusSupplierVariableListSolution, Long> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        var problem = new TestdataQuarkusSupplierVariableListSolution();
        problem.setEntityList(List.of(new TestdataQuarkusSupplierVariableListEntity("a"),
                new TestdataQuarkusSupplierVariableListEntity("b")));
        var a = new TestdataQuarkusSupplierVariableListValue("1");
        var b = new TestdataQuarkusSupplierVariableListValue("2");
        var c = new TestdataQuarkusSupplierVariableListValue("3", List.of(a, b));
        problem.setValueList(List.of(a, b, c));
        var solverJob = solverManager.solve(1L, problem);
        var solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertNotSame(solution, problem);
        assertEquals(-1, solution.getScore().score());
        assertNotSame(solution.getEntityList().get(0), problem.getEntityList().get(1));
    }

}
