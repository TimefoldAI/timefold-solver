package ai.timefold.solver.benchmark.api;

import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * If at least one of the {@link SingleBenchmarkResult}s of a {@link PlannerBenchmark} fail,
 * the {@link PlannerBenchmark} throws this exception
 * after all {@link SingleBenchmarkResult}s are finished and the benchmark report has been written.
 */
public class PlannerBenchmarkException extends RuntimeException {

    public PlannerBenchmarkException(@NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

}
