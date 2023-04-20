package ai.timefold.solver.examples.tsp.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.tsp.app.TspApp;
import ai.timefold.solver.examples.tsp.domain.TspSolution;

class TspImporterTest extends ImportDataFilesTest<TspSolution> {

    @Override
    protected AbstractSolutionImporter<TspSolution> createSolutionImporter() {
        return new TspImporter();
    }

    @Override
    protected String getDataDirName() {
        return TspApp.DATA_DIR_NAME;
    }
}
