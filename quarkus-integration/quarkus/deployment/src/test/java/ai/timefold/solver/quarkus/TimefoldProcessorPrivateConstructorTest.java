package ai.timefold.solver.quarkus;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorConstraintProvider;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorEntity;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorPrivateConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.optaplanner.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PrivateNoArgsConstructorConstraintProvider.class,
                            PrivateNoArgsConstructorSolution.class,
                            PrivateNoArgsConstructorEntity.class));

    @Inject
    SolverManager<PrivateNoArgsConstructorSolution, Long> solverManager;

    @Test
    void canConstructBeansWithPrivateConstructors() throws ExecutionException, InterruptedException {
        PrivateNoArgsConstructorSolution problem = new PrivateNoArgsConstructorSolution(
                Arrays.asList(
                        new PrivateNoArgsConstructorEntity("1"),
                        new PrivateNoArgsConstructorEntity("2"),
                        new PrivateNoArgsConstructorEntity("3")));
        PrivateNoArgsConstructorSolution solution = solverManager.solve(1L, problem).getFinalBestSolution();
        Assertions.assertEquals(solution.score.score(), 0);
        Assertions.assertEquals(solution.someField, 2);
    }

}
