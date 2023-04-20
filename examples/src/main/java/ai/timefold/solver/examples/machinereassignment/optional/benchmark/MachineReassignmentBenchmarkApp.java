package ai.timefold.solver.examples.machinereassignment.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class MachineReassignmentBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new MachineReassignmentBenchmarkApp().buildAndBenchmark(args);
    }

    public MachineReassignmentBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/machinereassignment/optional/benchmark/machineReassignmentBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/machinereassignment/optional/benchmark/machineReassignmentStepLimitBenchmarkConfig.xml"),
                new ArgOption("scoreDirector",
                        "ai/timefold/solver/examples/machinereassignment/optional/benchmark/machineReassignmentScoreDirectorBenchmarkConfig.xml"),
                new ArgOption("template",
                        "ai/timefold/solver/examples/machinereassignment/optional/benchmark/machineReassignmentBenchmarkConfigTemplate.xml.ftl",
                        true));
    }

}
