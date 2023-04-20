package ai.timefold.solver.examples.examination.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.examination.app.ExaminationApp;
import ai.timefold.solver.examples.examination.domain.Examination;

class ExaminationImporterTest extends ImportDataFilesTest<Examination> {

    @Override
    protected AbstractSolutionImporter<Examination> createSolutionImporter() {
        return new ExaminationImporter();
    }

    @Override
    protected String getDataDirName() {
        return ExaminationApp.DATA_DIR_NAME;
    }
}
