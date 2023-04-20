package ai.timefold.solver.examples.travelingtournament.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class TravelingTournamentBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new TravelingTournamentBenchmarkApp().buildAndBenchmark(args);
    }

    public TravelingTournamentBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/travelingtournament/optional/benchmark/travelingTournamentBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/travelingtournament/optional/benchmark/travelingTournamentStepLimitBenchmarkConfig.xml"));
    }

}
