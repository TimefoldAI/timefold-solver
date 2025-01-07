package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Level;

import ai.timefold.solver.quarkus.testdata.normal.constraints.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorWarningBuildTimePropertyChangedTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.timefold.solver.daemon", "true")
            // We overwrite the value at runtime
            .overrideRuntimeConfigKey("quarkus.timefold.solver.daemon", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataQuarkusEntity.class, TestdataQuarkusSolution.class,
                            TestdataQuarkusConstraintProvider.class))
            // Make sure Quarkus does not produce a warning for overwriting a build time value at runtime
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkus")
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                assertEquals(1, logRecords.size(), "expected warning to be generated");
            });

    @Test
    void solverProperties() {
        // Test is done by assertLogRecords
    }
}
