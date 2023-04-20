package ai.timefold.solver.quarkus;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdata.gizmo.OnlyMultiArgsConstructorEntity;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorConstraintProvider;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorEntity;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorOnlyMultiConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PrivateNoArgsConstructorConstraintProvider.class,
                            PrivateNoArgsConstructorSolution.class,
                            PrivateNoArgsConstructorEntity.class,
                            OnlyMultiArgsConstructorEntity.class))
            .assertException(throwable -> {
                Assertions.assertEquals(
                        "Class (" + OnlyMultiArgsConstructorEntity.class.getName()
                                + ") must have a no-args constructor so it can be constructed by Timefold.",
                        throwable.getMessage());
            });

    @Inject
    SolverManager<PrivateNoArgsConstructorSolution, Long> solverManager;

    @Test
    void canConstructBeansWithPrivateConstructors() throws ExecutionException, InterruptedException {
    }

}
