package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdata.suppliervariable.constraints.TestdataQuarkusSupplierVariableConstraintProvider;
import ai.timefold.solver.quarkus.testdata.suppliervariable.domain.TestdataQuarkusSupplierVariableEntity;
import ai.timefold.solver.quarkus.testdata.suppliervariable.domain.TestdataQuarkusSupplierVariableSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorSupplierShadowSolutionSolveTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.enabled-preview-features", "DECLARATIVE_SHADOW_VARIABLES")
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusSupplierVariableSolution.class,
                            TestdataQuarkusSupplierVariableEntity.class,
                            TestdataQuarkusSupplierVariableConstraintProvider.class));
    @Inject
    SolverManager<TestdataQuarkusSupplierVariableSolution, Long> solverManager;

    @Test
    void solve() throws ExecutionException, InterruptedException {
        var shadowEntity =
                new TestdataQuarkusSupplierVariableEntity();
        var problem = new TestdataQuarkusSupplierVariableSolution();
        problem.setEntityList(List.of(shadowEntity));
        problem.setValueList(List.of("a", "b"));
        var solverJob = solverManager.solve(1L, problem);
        var solution = solverJob.getFinalBestSolution();
        assertNotNull(solution);
        assertNotSame(solution, problem);
        assertEquals(0, solution.getScore().score());
        assertNotSame(solution.getEntityList().get(0), problem.getEntityList().get(0));
    }

}
