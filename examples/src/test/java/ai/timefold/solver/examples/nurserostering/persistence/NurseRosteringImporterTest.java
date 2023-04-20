package ai.timefold.solver.examples.nurserostering.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.nurserostering.app.NurseRosteringApp;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;

class NurseRosteringImporterTest extends ImportDataFilesTest<NurseRoster> {

    @Override
    protected AbstractSolutionImporter<NurseRoster> createSolutionImporter() {
        return new NurseRosteringImporter();
    }

    @Override
    protected String getDataDirName() {
        return NurseRosteringApp.DATA_DIR_NAME;
    }
}
