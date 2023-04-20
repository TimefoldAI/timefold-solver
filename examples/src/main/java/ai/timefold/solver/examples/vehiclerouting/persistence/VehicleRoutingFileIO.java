package ai.timefold.solver.examples.vehiclerouting.persistence;

import java.io.File;

import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class VehicleRoutingFileIO implements SolutionFileIO<VehicleRoutingSolution> {

    private VehicleRoutingImporter importer = new VehicleRoutingImporter();

    @Override
    public String getInputFileExtension() {
        return "vrp";
    }

    @Override
    public VehicleRoutingSolution read(File inputSolutionFile) {
        return importer.readSolution(inputSolutionFile);
    }

    @Override
    public void write(VehicleRoutingSolution solution, File outputSolutionFile) {
        throw new UnsupportedOperationException();
    }

}
