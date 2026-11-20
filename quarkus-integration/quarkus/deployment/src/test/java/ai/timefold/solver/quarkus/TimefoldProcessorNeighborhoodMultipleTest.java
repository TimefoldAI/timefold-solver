package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.quarkus.testdomain.neighborhood.OtherQuarkusMoveSelectionStrategy;
import ai.timefold.solver.quarkus.testdomain.neighborhood.TestdataQuarkusMoveSelectionStrategy;
import ai.timefold.solver.quarkus.testdomain.neighborhood.TestdataQuarkusNeighborhoodProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorNeighborhoodMultipleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.enabled-preview-features", "NEIGHBORHOODS")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class)
                    .addClasses(TestdataQuarkusNeighborhoodProvider.class, TestdataQuarkusMoveSelectionStrategy.class,
                            OtherQuarkusMoveSelectionStrategy.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple classes")
                    .hasMessageContaining("TestdataQuarkusMoveSelectionStrategy")
                    .hasMessageContaining("OtherQuarkusMoveSelectionStrategy")
                    .hasMessageContaining("MoveSelectionStrategy"));

    @Test
    void test() {
        throw new IllegalStateException("The build should have failed.");
    }

}
