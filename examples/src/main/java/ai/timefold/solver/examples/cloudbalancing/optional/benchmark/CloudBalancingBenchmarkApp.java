package ai.timefold.solver.examples.cloudbalancing.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class CloudBalancingBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new CloudBalancingBenchmarkApp().buildAndBenchmark(args);
    }

    public CloudBalancingBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/cloudbalancing/optional/benchmark/cloudBalancingBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/cloudbalancing/optional/benchmark/cloudBalancingStepLimitBenchmarkConfig.xml"),
                new ArgOption("scoreDirector",
                        "ai/timefold/solver/examples/cloudbalancing/optional/benchmark/cloudBalancingScoreDirectorBenchmarkConfig.xml"),
                new ArgOption("template",
                        "ai/timefold/solver/examples/cloudbalancing/optional/benchmark/cloudBalancingBenchmarkConfigTemplate.xml.ftl",
                        true),
                new ArgOption("partitioned",
                        "ai/timefold/solver/examples/cloudbalancing/optional/benchmark/cloudBalancingPartitionedSearchBenchmarkConfig.xml"));
    }
}
