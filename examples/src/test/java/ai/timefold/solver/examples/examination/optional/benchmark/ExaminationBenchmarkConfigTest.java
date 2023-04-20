package ai.timefold.solver.examples.examination.optional.benchmark;

import ai.timefold.solver.examples.common.app.AbstractBenchmarkConfigTest;
import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

class ExaminationBenchmarkConfigTest extends AbstractBenchmarkConfigTest {

    @Override
    protected CommonBenchmarkApp getBenchmarkApp() {
        return new ExaminationBenchmarkApp();
    }
}
