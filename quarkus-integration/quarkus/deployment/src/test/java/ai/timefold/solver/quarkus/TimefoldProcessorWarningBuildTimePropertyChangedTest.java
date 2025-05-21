package ai.timefold.solver.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Level;

import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusConstraintProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

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
            .setLogRecordPredicate(logRecord -> logRecord.getLoggerName().startsWith("io.quarkus")
                    && logRecord.getLevel().intValue() >= Level.WARNING.intValue());

    @Test
    void solverProperties() {
        config.assertLogRecords(logRecords -> {
            assertEquals(1, logRecords.size(), "expected warning to be generated");
        });
    }
}
