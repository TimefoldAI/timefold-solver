package ai.timefold.solver.examples.vehiclerouting.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.vehiclerouting.app.VehicleRoutingApp;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;

class VehicleRoutingImportDataFilesTest extends ImportDataFilesTest<VehicleRoutingSolution> {

    @Override
    protected AbstractSolutionImporter<VehicleRoutingSolution> createSolutionImporter() {
        return new VehicleRoutingImporter();
    }

    @Override
    protected String getDataDirName() {
        return VehicleRoutingApp.DATA_DIR_NAME;
    }
}
