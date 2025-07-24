package ai.timefold.solver.core;

import org.junit.jupiter.api.Test;

public class FailingTest {

    @Test
    void failingTest() {
        // This test is intentionally failing to demonstrate the failure handling in the test suite.
        throw new AssertionError("This test is designed to fail.");
    }

}
