package ai.timefold.solver.examples.cloudbalancing.app;

import ai.timefold.solver.examples.cloudbalancing.optional.benchmark.CloudBalancingBenchmarkApp;
import ai.timefold.solver.examples.common.app.AbstractBenchmarkConfigTest;
import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

class CloudBalancingBenchmarkConfigTest extends AbstractBenchmarkConfigTest {

    @Override
    protected CommonBenchmarkApp getBenchmarkApp() {
        return new CloudBalancingBenchmarkApp();
    }
}
