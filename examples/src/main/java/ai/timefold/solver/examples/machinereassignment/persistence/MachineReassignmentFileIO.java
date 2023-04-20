package ai.timefold.solver.examples.machinereassignment.persistence;

import java.io.File;

import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class MachineReassignmentFileIO implements SolutionFileIO<MachineReassignment> {

    private MachineReassignmentImporter importer = new MachineReassignmentImporter();
    private MachineReassignmentExporter exporter = new MachineReassignmentExporter();

    @Override
    public String getInputFileExtension() {
        return "txt";
    }

    @Override
    public MachineReassignment read(File inputSolutionFile) {
        return importer.readSolution(inputSolutionFile);
    }

    @Override
    public void write(MachineReassignment solution, File outputSolutionFile) {
        exporter.writeSolution(solution, outputSolutionFile);
    }

}
