package ai.timefold.solver.benchmark.config.blueprint;

import java.util.List;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "solverBenchmarkBluePrintType"
})
public class SolverBenchmarkBluePrintConfig {

    protected SolverBenchmarkBluePrintType solverBenchmarkBluePrintType = null;

    public @Nullable SolverBenchmarkBluePrintType getSolverBenchmarkBluePrintType() {
        return solverBenchmarkBluePrintType;
    }

    public void setSolverBenchmarkBluePrintType(@Nullable SolverBenchmarkBluePrintType solverBenchmarkBluePrintType) {
        this.solverBenchmarkBluePrintType = solverBenchmarkBluePrintType;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public @NonNull List<SolverBenchmarkConfig> buildSolverBenchmarkConfigList() {
        validate();
        return solverBenchmarkBluePrintType.buildSolverBenchmarkConfigList();
    }

    protected void validate() {
        if (solverBenchmarkBluePrintType == null) {
            throw new IllegalArgumentException(
                    "The solverBenchmarkBluePrint must have"
                            + " a solverBenchmarkBluePrintType (" + solverBenchmarkBluePrintType + ").");
        }
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SolverBenchmarkBluePrintConfig withSolverBenchmarkBluePrintType(
            @NonNull SolverBenchmarkBluePrintType solverBenchmarkBluePrintType) {
        this.solverBenchmarkBluePrintType = solverBenchmarkBluePrintType;
        return this;
    }

}
