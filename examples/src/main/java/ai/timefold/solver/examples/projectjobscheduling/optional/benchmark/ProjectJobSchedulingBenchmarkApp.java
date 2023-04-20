package ai.timefold.solver.examples.projectjobscheduling.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class ProjectJobSchedulingBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new ProjectJobSchedulingBenchmarkApp().buildAndBenchmark(args);
    }

    public ProjectJobSchedulingBenchmarkApp() {
        super(
                new ArgOption("template",
                        "ai/timefold/solver/examples/projectjobscheduling/optional/benchmark/projectJobSchedulingBenchmarkConfigTemplate.xml.ftl",
                        true));
    }

}
