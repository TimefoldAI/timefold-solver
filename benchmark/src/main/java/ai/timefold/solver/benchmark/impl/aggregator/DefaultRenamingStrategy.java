package ai.timefold.solver.benchmark.impl.aggregator;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;

public class DefaultRenamingStrategy implements SolverBenchmarkRenamingStrategy {
    @Override
    public void rename(SolverBenchmarkResult result, String newName) {
        if (!result.getName().equals(newName)) {
            result.setName(newName);
        }
    }
}

