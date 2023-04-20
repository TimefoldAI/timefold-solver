package ai.timefold.solver.examples.conferencescheduling.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class ConferenceSchedulingBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new ConferenceSchedulingBenchmarkApp().buildAndBenchmark(args);
    }

    public ConferenceSchedulingBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/conferencescheduling/optional/benchmark/conferenceSchedulingBenchmarkConfig.xml"));
    }

}
