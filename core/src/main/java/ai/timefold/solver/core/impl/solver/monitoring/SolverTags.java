package ai.timefold.solver.core.impl.solver.monitoring;

import java.time.Clock;
import java.time.ZoneOffset;

import ai.timefold.solver.core.api.solver.SolverManager;

import org.jspecify.annotations.NullMarked;

import io.micrometer.core.instrument.Tags;

/**
 * Some Micrometer registries like Prometheus require each meter
 * to be registered with the same tag key set. In order to enforce
 * each {@link Tags} instance having the same keys, this class is
 * to be used instead of creating the {@link Tags} manually.
 *
 * @param problemId The problem id passed to {@link SolverManager}, or a benchmark id
 */
@NullMarked
public record SolverTags(Object problemId) {
    public Tags asTags() {
        return Tags.of("problem.id", problemId.toString());
    }

    public static SolverTags withProblemId(Object problemId) {
        return new SolverTags(problemId);
    }

    public static SolverTags withoutProblemId(Clock clock) {
        return new SolverTags(clock.instant().atOffset(ZoneOffset.UTC));
    }
}
