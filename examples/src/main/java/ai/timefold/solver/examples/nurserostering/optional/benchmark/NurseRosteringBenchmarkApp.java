package ai.timefold.solver.examples.nurserostering.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class NurseRosteringBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new NurseRosteringBenchmarkApp().buildAndBenchmark(args);
    }

    public NurseRosteringBenchmarkApp() {
        super(
                new ArgOption("sprint",
                        "ai/timefold/solver/examples/nurserostering/optional/benchmark/nurseRosteringSprintBenchmarkConfig.xml"),
                new ArgOption("medium",
                        "ai/timefold/solver/examples/nurserostering/optional/benchmark/nurseRosteringMediumBenchmarkConfig.xml"),
                new ArgOption("long",
                        "ai/timefold/solver/examples/nurserostering/optional/benchmark/nurseRosteringLongBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/nurserostering/optional/benchmark/nurseRosteringStepLimitBenchmarkConfig.xml"));
    }

}
