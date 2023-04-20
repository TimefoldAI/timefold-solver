package ai.timefold.solver.examples.pas.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class PatientAdmissionScheduleBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new PatientAdmissionScheduleBenchmarkApp().buildAndBenchmark(args);
    }

    public PatientAdmissionScheduleBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/pas/optional/benchmark/patientAdmissionScheduleBenchmarkConfig.xml"));
    }

}
