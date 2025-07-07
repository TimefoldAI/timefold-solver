
package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.quarkus.testdomain.suppliervariable.missing.TestdataQuarkusDeclarativeMissingSupplierEasyScoreCalculator;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.missing.TestdataQuarkusDeclarativeMissingSupplierEntity;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.missing.TestdataQuarkusDeclarativeMissingSupplierSolution;
import ai.timefold.solver.quarkus.testdomain.suppliervariable.missing.TestdataQuarkusDeclarativeMissingSupplierValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorMissingSupplierForDeclarativeVariableTest {

    // Empty classes
    @RegisterExtension
    static final QuarkusUnitTest config1 = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(
                    TestdataQuarkusDeclarativeMissingSupplierSolution.class,
                    TestdataQuarkusDeclarativeMissingSupplierEntity.class,
                    TestdataQuarkusDeclarativeMissingSupplierValue.class,
                    TestdataQuarkusDeclarativeMissingSupplierEasyScoreCalculator.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(
                            "@ShadowVariable (endTime)",
                            "supplierMethod (calculateEndTime) that does not exist",
                            "inside its declaring class (ai.timefold.solver.quarkus.testdomain.suppliervariable.missing.TestdataQuarkusDeclarativeMissingSupplierValue).",
                            "Maybe you misspelled the supplierMethod name?"));

    @Test
    void test() {
        fail("Should not call this method.");
    }
}
