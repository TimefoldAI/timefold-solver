package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.quarkus.testdata.gizmo.DummyConstraintProvider;
import ai.timefold.solver.quarkus.testdata.gizmo.DummyVariableListener;
import ai.timefold.solver.quarkus.testdata.gizmo.TestDataKitchenSinkAutoDiscoverFieldSolution;
import ai.timefold.solver.quarkus.testdata.gizmo.TestDataKitchenSinkEntity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorGizmoKitchenSinkAutoDiscoverFieldTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.termination.best-score-limit", "0hard/0soft")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestDataKitchenSinkAutoDiscoverFieldSolution.class,
                            TestDataKitchenSinkEntity.class,
                            DummyConstraintProvider.class,
                            DummyVariableListener.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("autoDiscoverMemberType"));

    @Test
    void solve() { // The method exists only so that the class is considered a test.
        throw new IllegalStateException("The test is expected to fail before it even gets here.");
    }

}
