package ai.timefold.solver.benchmark.impl.aggregator;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;

public interface SolverBenchmarkRenamingStrategy {
    void rename(SolverBenchmarkResult result, String newName);
}
