package ai.timefold.solver.examples.machinereassignment.persistence;

import java.io.File;
import java.util.function.Predicate;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.machinereassignment.app.MachineReassignmentApp;
import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;

class MachineReassignmentImporterTest extends ImportDataFilesTest<MachineReassignment> {

    @Override
    protected AbstractSolutionImporter<MachineReassignment> createSolutionImporter() {
        return new MachineReassignmentImporter();
    }

    @Override
    protected String getDataDirName() {
        return MachineReassignmentApp.DATA_DIR_NAME;
    }

    @Override
    protected Predicate<File> dataFileInclusionFilter() {
        // The dataset B10 requires more than 1GB heap space on JDK 6 to load (not on JDK 7)
        return file -> !file.getName().equals("model_b_10.txt");
    }
}
