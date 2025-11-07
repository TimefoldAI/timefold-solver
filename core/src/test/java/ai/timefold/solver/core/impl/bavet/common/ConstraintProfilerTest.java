package ai.timefold.solver.core.impl.bavet.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.config.score.director.ConstraintProfilingMode;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MockClock;

class ConstraintProfilerTest {
    final String constraintProviderClassName = "MyConstraintProvider";
    final ConstraintNodeProfileId A_1 = new ConstraintNodeProfileId(0, Set.of(
            new ConstraintNodeLocation(constraintProviderClassName, "a", 1)));
    final ConstraintNodeProfileId A_2 = new ConstraintNodeProfileId(1, Set.of(
            new ConstraintNodeLocation(constraintProviderClassName, "a", 2)));
    final ConstraintNodeProfileId B_1 = new ConstraintNodeProfileId(2, Set.of(
            new ConstraintNodeLocation(constraintProviderClassName, "b", 1)));
    final ConstraintNodeProfileId B_2 = new ConstraintNodeProfileId(3, Set.of(
            new ConstraintNodeLocation(constraintProviderClassName, "b", 2)));
    final ConstraintNodeProfileId AB_3 = new ConstraintNodeProfileId(4, Set.of(
            new ConstraintNodeLocation(constraintProviderClassName, "a", 3),
            new ConstraintNodeLocation(constraintProviderClassName, "b", 3)));

    ConstraintProfiler constraintProfiler;
    MockClock clock;

    void setUp(ConstraintProfilingMode constraintProfilingMode) {
        clock = new MockClock();
        constraintProfiler = new ConstraintProfiler(clock, constraintProfilingMode);
        List.of(A_1, A_2, B_1, B_2, AB_3).forEach(constraintProfiler::register);
    }

    Runnable advance(long seconds) {
        return () -> clock.addSeconds(seconds);
    }

    @Test
    void getSummaryByMethod() {
        setUp(ConstraintProfilingMode.BY_METHOD);

        constraintProfiler.measure(A_1, ConstraintProfiler.Operation.INSERT,
                advance(1));
        constraintProfiler.measure(A_2, ConstraintProfiler.Operation.RETRACT,
                advance(2));
        constraintProfiler.measure(B_1, ConstraintProfiler.Operation.UPDATE,
                advance(3));
        constraintProfiler.measure(B_2, ConstraintProfiler.Operation.INSERT,
                advance(4));
        constraintProfiler.measure(AB_3, ConstraintProfiler.Operation.INSERT,
                advance(5));
        constraintProfiler.measure(A_1, ConstraintProfiler.Operation.INSERT,
                advance(3));

        // Total A = 1 + 2 + 5 + 3 = 11
        // Total B = 3 + 4 + 5 = 12
        // Subtract 5 because A3 and B3 share the same node
        // Total = 11 + 12 - 5 = 18

        assertThat(constraintProfiler.getSummary())
                .isEqualTo("""
                        Constraint Profiling Summary
                        MyConstraintProvider#b 66.67%
                        MyConstraintProvider#a 61.11%""");
    }

    @Test
    void getSummaryByLine() {
        setUp(ConstraintProfilingMode.BY_LINE);

        constraintProfiler.measure(A_1, ConstraintProfiler.Operation.INSERT,
                advance(1));
        constraintProfiler.measure(A_2, ConstraintProfiler.Operation.RETRACT,
                advance(2));
        constraintProfiler.measure(B_1, ConstraintProfiler.Operation.UPDATE,
                advance(3));
        constraintProfiler.measure(B_2, ConstraintProfiler.Operation.INSERT,
                advance(4));
        constraintProfiler.measure(AB_3, ConstraintProfiler.Operation.INSERT,
                advance(5));
        constraintProfiler.measure(A_1, ConstraintProfiler.Operation.INSERT,
                advance(3));

        // Total A1 = 1 + 3 = 4
        // Total A2 = 2
        // Total A3 = 5
        // Total B1 = 3
        // Total B2 = 4
        // Total B3 = 5
        // Subtract 5 because A3 and B3 share the same node
        // Total = 4 + 2 + 3 + 4 + 5 + 5 - 5 = 18

        assertThat(constraintProfiler.getSummary())
                .isEqualTo("""
                        Constraint Profiling Summary
                        MyConstraintProvider#b:3 27.78%
                        MyConstraintProvider#a:3 27.78%
                        MyConstraintProvider#b:2 22.22%
                        MyConstraintProvider#a:1 22.22%
                        MyConstraintProvider#b:1 16.67%
                        MyConstraintProvider#a:2 11.11%""");
    }
}