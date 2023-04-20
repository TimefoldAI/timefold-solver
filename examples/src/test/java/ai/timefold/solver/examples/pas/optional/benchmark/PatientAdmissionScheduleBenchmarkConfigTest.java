package ai.timefold.solver.examples.pas.optional.benchmark;

import ai.timefold.solver.examples.common.app.AbstractBenchmarkConfigTest;
import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

class PatientAdmissionScheduleBenchmarkConfigTest extends AbstractBenchmarkConfigTest {

    @Override
    protected CommonBenchmarkApp getBenchmarkApp() {
        return new PatientAdmissionScheduleBenchmarkApp();
    }
}
