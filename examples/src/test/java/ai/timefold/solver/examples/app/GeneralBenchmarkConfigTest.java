package ai.timefold.solver.examples.app;

import ai.timefold.solver.examples.common.app.AbstractBenchmarkConfigTest;
import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

class GeneralBenchmarkConfigTest extends AbstractBenchmarkConfigTest {

    @Override
    protected CommonBenchmarkApp getBenchmarkApp() {
        return new GeneralTimefoldBenchmarkApp();
    }
}
