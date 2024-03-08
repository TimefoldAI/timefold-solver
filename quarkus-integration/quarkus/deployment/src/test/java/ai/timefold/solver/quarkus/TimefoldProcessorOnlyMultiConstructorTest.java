package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.testdata.gizmo.OnlyMultiArgsConstructorEntity;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorConstraintProvider;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorEntity;
import ai.timefold.solver.quarkus.testdata.gizmo.PrivateNoArgsConstructorSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .assertException(t -> assertThat(t).hasMessageContainingAll("Class (",
                    OnlyMultiArgsConstructorEntity.class.getName(),
                    ") must have a no-args constructor so it can be constructed by Timefold."));

    @Inject
    SolverManager<PrivateNoArgsConstructorSolution, Long> solverManager;

    @Test
    void canConstructBeansWithPrivateConstructors() {
    }

}
