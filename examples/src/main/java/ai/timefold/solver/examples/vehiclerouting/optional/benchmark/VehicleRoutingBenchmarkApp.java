package ai.timefold.solver.examples.vehiclerouting.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class VehicleRoutingBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new VehicleRoutingBenchmarkApp().buildAndBenchmark(args);
    }

    public VehicleRoutingBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/vehiclerouting/optional/benchmark/vehicleRoutingBenchmarkConfig.xml"),
                new ArgOption("scoreDirector",
                        "ai/timefold/solver/examples/vehiclerouting/optional/benchmark/vehicleRoutingScoreDirectorBenchmarkConfig.xml"));
    }

}
