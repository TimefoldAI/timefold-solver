package ai.timefold.solver.examples.tsp.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class TspBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new TspBenchmarkApp().buildAndBenchmark(args);
    }

    public TspBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/tsp/optional/benchmark/tspBenchmarkConfig.xml"));
    }

}
