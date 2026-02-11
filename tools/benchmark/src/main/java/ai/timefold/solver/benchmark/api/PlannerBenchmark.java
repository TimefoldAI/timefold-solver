package ai.timefold.solver.benchmark.api;

import java.io.File;

import org.jspecify.annotations.NonNull;

/**
 * A planner benchmark that runs a number of single benchmarks.
 * <p>
 * Build by a {@link PlannerBenchmarkFactory}.
 */
public interface PlannerBenchmark {

    /**
     * Run all the single benchmarks and create an overview report.
     *
     * @return the directory in which the benchmark results are stored
     */
    @NonNull
    File benchmark();

}
