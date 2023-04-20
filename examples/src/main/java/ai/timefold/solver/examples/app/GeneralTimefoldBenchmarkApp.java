package ai.timefold.solver.examples.app;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class GeneralTimefoldBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new GeneralTimefoldBenchmarkApp().buildAndBenchmark(args);
    }

    public GeneralTimefoldBenchmarkApp() {
        super(
                new ArgOption("default",
                        "org/optaplanner/examples/app/benchmark/generalOptaPlannerBenchmarkConfig.xml"),
                new ArgOption("template",
                        "org/optaplanner/examples/app/benchmark/generalOptaPlannerBenchmarkConfigTemplate.xml.ftl", true));
    }

}
