package ai.timefold.solver.examples.common;

public final class TestSystemProperties {

    public static final String MOVE_THREAD_COUNTS = "moveThreadCounts";
    /**
     * Use name of the example (eg. "cloudbalancing") or "all" to run all turtle tests.
     * Not providing this property will skip turtle tests.
     */
    public static final String TURTLE_TEST_SELECTION = "ai.timefold.solver.examples.turtle";
    public static final String TURTLE_TEST_RUN_TIME_LIMIT = "ai.timefold.solver.examples.turtle.runTimeLimitMinutes";
    public static final String MOVE_THREAD_COUNT = "moveThreadCount";

    private TestSystemProperties() {
    }
}
