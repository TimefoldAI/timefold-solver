package ai.timefold.solver.examples.app;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class GeneralTimefoldBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new GeneralTimefoldBenchmarkApp().buildAndBenchmark(args);
    }

    public GeneralTimefoldBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/app/benchmark/generalTimefoldBenchmarkConfig.xml"),
                new ArgOption("template",
                        "ai/timefold/solver/examples/app/benchmark/generalTimefoldBenchmarkConfigTemplate.xml.ftl", true));
    }

}
