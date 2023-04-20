package ai.timefold.solver.examples.vehiclerouting.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.vehiclerouting.app.VehicleRoutingApp;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;

class VehicleRoutingOpenDataFilesTest extends OpenDataFilesTest<VehicleRoutingSolution> {

    @Override
    protected CommonApp<VehicleRoutingSolution> createCommonApp() {
        return new VehicleRoutingApp();
    }
}
