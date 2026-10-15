package ai.timefold.solver.migration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
public abstract class AbstractRecipeTest implements RewriteTest {

    @BeforeAll
    static void skipOnUnsupportedJdk() {
        // OpenRewrite supports only LTS JDKs.
        assumeTrue(Runtime.version().feature() <= 25, "OpenRewrite does not support JDK > 25.");
    }

}
