package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.timefold.solver.core.impl.testdata.constraints.DummyConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.immutable.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.immutable.record.TestdataRecordEntity;
import ai.timefold.solver.core.impl.testdata.domain.immutable.record.TestdataRecordValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorImmutablePlanningEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyConstraintProvider.class, TestdataSolution.class,
                            TestdataRecordEntity.class, TestdataRecordValue.class))
            .assertException(exception -> {
                assertEquals(IllegalArgumentException.class, exception.getClass());
                assertTrue(exception.getMessage().contains("cannot be a record as it needs to be mutable"));
            });

    @Test
    void solve() {
        fail("Build should fail");
    }

}
