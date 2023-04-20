package ai.timefold.solver.examples.examination.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class ExaminationBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new ExaminationBenchmarkApp().buildAndBenchmark(args);
    }

    public ExaminationBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/examination/optional/benchmark/examinationBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/examination/optional/benchmark/examinationStepLimitBenchmarkConfig.xml"));
    }

}
