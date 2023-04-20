package ai.timefold.solver.examples.pas.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.pas.app.PatientAdmissionScheduleApp;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;

class PatientAdmissionScheduleImporterTest extends ImportDataFilesTest<PatientAdmissionSchedule> {

    @Override
    protected AbstractSolutionImporter<PatientAdmissionSchedule> createSolutionImporter() {
        return new PatientAdmissionScheduleImporter();
    }

    @Override
    protected String getDataDirName() {
        return PatientAdmissionScheduleApp.DATA_DIR_NAME;
    }
}
