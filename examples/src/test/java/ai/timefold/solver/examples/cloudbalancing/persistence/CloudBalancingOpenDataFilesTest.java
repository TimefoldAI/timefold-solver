package ai.timefold.solver.examples.cloudbalancing.persistence;

import ai.timefold.solver.examples.cloudbalancing.app.CloudBalancingApp;
import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;

class CloudBalancingOpenDataFilesTest extends OpenDataFilesTest<CloudBalance> {

    @Override
    protected CommonApp<CloudBalance> createCommonApp() {
        return new CloudBalancingApp();
    }
}
