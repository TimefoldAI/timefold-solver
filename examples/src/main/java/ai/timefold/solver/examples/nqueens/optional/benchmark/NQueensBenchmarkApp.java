package ai.timefold.solver.examples.nqueens.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class NQueensBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new NQueensBenchmarkApp().buildAndBenchmark(args);
    }

    public NQueensBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/nqueens/optional/benchmark/nqueensBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/nqueens/optional/benchmark/nqueensStepLimitBenchmarkConfig.xml"),
                new ArgOption("scoreDirector",
                        "ai/timefold/solver/examples/nqueens/optional/benchmark/nqueensScoreDirectorBenchmarkConfig.xml"));
    }

}
